using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace plex_importer
{
    public class TVShow
    {
        public string PlexRoot = "/volume1/Plex/TV Shows";
        public string SeriesName { get; set; }
        public int SeasonNumber { get; set; }
        public int EpisodeNumber { get; set; }
        public string EpisodeTitle { get; set; } = null;
        public string NewFilename { get; set; }
        public string source { get; set; }
        public string destination { get; set; }

        public void Run()
        {
            Console.WriteLine($"TV Show: {source}");

            AskForSeriesName();
            AskForSeasonNumber();
            AskForEpisodeNumber();
            AskForEpisodeTitle();

            destination = $"{PlexRoot}/{SeriesName}/Season {ConvertNumber(SeasonNumber)}";

            GenerateNewFilename();
            MoveFile();
        }

        private void AskForSeriesName()
        {
            Console.WriteLine("What is the series name?");
            ReadLine.AutoCompletionHandler = new DirectorySelectionAutoCompletionHandler(PlexRoot);
            SeriesName = ReadLine.Read("> ");

            if (SeriesName == "New Series")
            {
                Console.WriteLine("What is the new series name?");
                ReadLine.AutoCompletionHandler = new DefaultAutoCompleteHandler();
                SeriesName = ReadLine.Read("> ");
                destination = $"{PlexRoot}/{SeriesName}";
                Console.WriteLine($"Making new directory: {destination}");
                System.IO.Directory.CreateDirectory(destination);
            }
        }

        private void AskForSeasonNumber()
        {
            Console.WriteLine("What is the season number?");
            SeasonNumber = Convert.ToInt32(ReadLine.Read("> "));
        }

        private void AskForEpisodeNumber()
        {
            Console.WriteLine("What is the episode number?");
            EpisodeNumber = Convert.ToInt32(ReadLine.Read("> "));
        }

        private void AskForEpisodeTitle()
        {
            Console.WriteLine("Do you know the episode title? (yes/no)");
            ReadLine.AutoCompletionHandler = new EpisodeTitleAutoCompleteHandler();
            string answer = ReadLine.Read("> ");

            if (answer == "no")
            {
                return;
            }

            Console.WriteLine("What is the episode title?");
            EpisodeTitle = ReadLine.Read("> ");
        }

        private void GenerateNewFilename()
        {
            string fileExtenstion = System.IO.Path.GetExtension(source);

            if (string.IsNullOrEmpty(EpisodeTitle))
            {
                NewFilename = $"{SeriesName} - s{ConvertNumber(SeasonNumber)}e{ConvertNumber(EpisodeNumber)}{fileExtenstion}";
                Console.WriteLine($"New Filename: {NewFilename}");
            }
            else
            {
                NewFilename = $"{SeriesName} - s{ConvertNumber(SeasonNumber)}e{ConvertNumber(EpisodeNumber)} - {EpisodeTitle}{fileExtenstion}";
                Console.WriteLine($"New Filename: {NewFilename}");
            }
        }

        private static string ConvertNumber(int number)
        {
            if (number < 10)
            {
                return $"0{number}";
            }
            else
            {
                return $"{number}";
            }
        }

        private void MoveFile()
        {
            if (System.IO.Directory.Exists(destination))
            {
                Console.WriteLine($"Moving {source} to {destination}/{NewFilename}");
                System.IO.File.Move(source, $"{destination}/{NewFilename}");
            }
            else
            {
                Console.WriteLine($"Making new directory: {destination}");
                System.IO.Directory.CreateDirectory(destination);
                Console.WriteLine($"Moving {source} to {destination}/{NewFilename}");
                System.IO.File.Move(source, $"{destination}/{NewFilename}");
            }
        }
    }

    class DirectorySelectionAutoCompletionHandler : IAutoCompleteHandler
    {
        private string _searchPath { get; set; }
        // characters to start completion from
        public char[] Separators { get; set; } = new char[0];

        public DirectorySelectionAutoCompletionHandler(string SearchPath)
        {
            _searchPath = SearchPath;
        }

        // text - The current text entered in the console
        // index - The index of the terminal cursor within {text}
        public string[] GetSuggestions(string text, int index)
        {
            List<string> directories = new List<string>();
            string[] directoryEntries = System.IO.Directory.GetDirectories(_searchPath).Where(f => f.ToLower().Contains(text)).ToArray();

            foreach (string directoryName in directoryEntries)
            {
                directories.Add(directoryName.Split('/').Last());
            }

            directories.Add("New Series");

            return directories.ToArray();
        }
    }

    public class EpisodeTitleAutoCompleteHandler : IAutoCompleteHandler
    {
        // characters to start completion from
        public char[] Separators { get; set; } = new char[0];

        // text - The current text entered in the console
        // index - The index of the terminal cursor within {text}
        public string[] GetSuggestions(string text, int index)
        {
            return new string[] { "no", "yes" };
        }
    }
}