using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace plex_importer
{
    public class Movies
    {
        public string PlexRoot = "/volume1/Plex/Movies";
        public string Title { get; set; }
        public string Year { get; set; }
        public string source { get; set; }
        public string destination { get; set; }
        public string NewFilename { get; set; }


        public void Run()
        {
            Console.WriteLine($"Movie: {source}");

            AskForMovieTitle();
            AskForMovieYear();

            destination = $"{PlexRoot}/{Title} ({Year})";

            MakeNewMovieDirectory();
            GenerateNewFilename();
            MoveMovie();
        }

        private void AskForMovieTitle()
        {
            Console.WriteLine("What is the movie's title?");
            Title = ReadLine.Read("> ");
        }

        private void AskForMovieYear()
        {
            Console.WriteLine("What is the movie's year?");
            Year = ReadLine.Read("> ");
        }

        private void MakeNewMovieDirectory()
        {
            Console.WriteLine($"Making new directory: {destination}");
            System.IO.Directory.CreateDirectory(destination);
        }

        private void GenerateNewFilename()
        {
            string fileExtension = System.IO.Path.GetExtension(source);
            NewFilename = $"{Title} ({Year}){fileExtension}";
            Console.WriteLine($"New Filename: {NewFilename}");
        }

        private void MoveMovie()
        {
            Console.WriteLine($"Moving {source} to {destination}/{NewFilename}");
            System.IO.File.Move(source, $"{destination}/{NewFilename}");
        }
    }
}
