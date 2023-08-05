using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace plex_importer;

public class Search
{
    public string SearchPath { get; set; }

    public Search(string SearchPath)
    {
        if (System.IO.Directory.Exists(SearchPath))
        {
            this.SearchPath = SearchPath;
        }
        else
        {
            throw new Exception("Search path does not exist.");
        }
    }

    public void Run()
    {
        Console.WriteLine("Search Path: {0}", SearchPath);

        // search for the directories
        List<string> directories = SearchForDirectories();

        // foreach loop on the directories
        foreach (string directory in directories)
        {
            // ask if it's a tv show or movie
            string tvShowOrMovie = AskForTVShowOrMovie(directory);

            if (tvShowOrMovie == "skip")
            {
                continue;
            }

            // get the files listed in the directory and ask for the video file
            string selectedFile = SelectFile(directory);
            // Console.WriteLine($"User Selected File: { selectedFile }");
            ReadLine.AutoCompletionHandler = new DefaultAutoCompleteHandler();

            // run the tv show or movie class for the selected file
            if (tvShowOrMovie == "tvshow")
            {
                TVShow tvShow = new TVShow();
                tvShow.source = selectedFile;
                tvShow.Run();
            }
            else
            {
                Movies movie = new Movies();
                movie.source = selectedFile;
                movie.Run();
            }

            System.IO.Directory.Delete(directory, true);  // true enables recursive deletion
        }
    }

    private string AskForTVShowOrMovie(string directoryName)
    {
        Console.WriteLine($"Movie or TV Show?");
        directoryName = directoryName.Split('/').Last();
        string tvShowOrMovie = "";
        ReadLine.AutoCompletionHandler = new TVorMovieAutoCompleteHandler();
        tvShowOrMovie = ReadLine.Read($"({ directoryName })> ");
        return tvShowOrMovie;
    }

    private string SelectFile(string directory)
    {
        string selectedFile = "";
        ReadLine.AutoCompletionHandler = new FileSelectionAutoCompletionHandler(directory);
        Console.WriteLine("Select the video file:");
        selectedFile = ReadLine.Read("> ");
        return selectedFile;
    }

    private List<string> SearchForFiles()
    {
        List<string> files = new List<string>();
        string[] fileEntries = System.IO.Directory.GetFiles(SearchPath);
        foreach (string fileName in fileEntries)
        {
            files.Add(fileName);
        }
        return files;
    }

    private List<string> SearchForDirectories()
    {
        List<string> directories = new List<string>();
        string[] directoryEntries = System.IO.Directory.GetDirectories(SearchPath);
        foreach (string directoryName in directoryEntries)
        {
            directories.Add(directoryName);
        }
        return directories;
    }
}

class DefaultAutoCompleteHandler : IAutoCompleteHandler
{
    // characters to start completion from
    public char[] Separators { get; set; } = new char[] { ' ', '.', '/' };

    // text - The current text entered in the console
    // index - The index of the terminal cursor within {text}
    public string[] GetSuggestions(string text, int index)
    {
        return null;
    }
}

class TVorMovieAutoCompleteHandler : IAutoCompleteHandler
{
    // characters to start completion from
    public char[] Separators { get; set; } = new char[] { ' ', '.', '/' };

    // text - The current text entered in the console
    // index - The index of the terminal cursor within {text}
    public string[] GetSuggestions(string text, int index)
    {
        var options = new string[] { "tvshow", "movie", "skip" };

        return options.Where(f => f.ToLower().Contains(text)).ToArray();
    }
}

class FileSelectionAutoCompletionHandler : IAutoCompleteHandler
{
    public string SearchPath { get; set; }
    // characters to start completion from
    public char[] Separators { get; set; } = new char[] { ' ', '.', '/' };

    public FileSelectionAutoCompletionHandler(string SearchPath)
    {
        this.SearchPath = SearchPath;
    }

    // text - The current text entered in the console
    // index - The index of the terminal cursor within {text}
    public string[] GetSuggestions(string text, int index)
    {
        return System.IO.Directory.GetFiles(SearchPath);
    }
}