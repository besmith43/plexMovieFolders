using System;
using System.IO;
using System.Threading.Tasks;

namespace plexMovieFolders.Class
{
    public class Class1
    {
        public int Add(int a, int b)
        {
            return a + b;
        }

        public void RunSerial(string plexRootDirectory)
        {
            string localPlexRootDirectory = conditionRootDirectory(plexRootDirectory);

            //movieFiles contains the full path and the file name
            string[] movieFiles = Directory.GetFiles(localPlexRootDirectory);

            foreach(string movieFile in movieFiles)
            {
                organizeMovie(movieFile, localPlexRootDirectory);
            }
            //organizeMovie(movieFiles[0], localPlexRootDirectory);
        }

        public void RunParallel(string plexRootDirectory)
        {
            string localPlexRootDirectory = conditionRootDirectory(plexRootDirectory);

            string[] movieFiles = Directory.GetFiles(localPlexRootDirectory);

            Parallel.ForEach(movieFiles, (movieFile) => {
                organizeMovie(movieFile, localPlexRootDirectory);
            });
        }

        private void organizeMovie(string movie, string rootDirectory)
        {
            //Console.WriteLine(movie);

            string movieFilename = movie.Substring(rootDirectory.Length+1);
            //Console.WriteLine(movieFilename);
            string movieName = movieFilename.Substring(0, movieFilename.Length-4);
            string movieExtension = movieFilename.Substring(movieFilename.Length-3);
            string newDirectoryName = conditionNewDirectoryName(movieName);
            //Console.WriteLine($"Movie Name: { movieName }");
            //make new directory
            //Console.WriteLine($"Making new directory: { rootDirectory }/{ newDirectoryName }");
            Directory.CreateDirectory($"{ rootDirectory }/{newDirectoryName }");
            //move movie into new directory
            //Console.WriteLine($"Move to new directory: { movie } to { rootDirectory }/{ newDirectoryName }/{ newDirectoryName }.{ movieExtension }");
            File.Move(movie, $"{ rootDirectory }/{ newDirectoryName }/{ newDirectoryName }.{ movieExtension }");
        }

        private string conditionRootDirectory(string rootDirectory)
        {
            if (rootDirectory[rootDirectory.Length-1] == '/')
            {
                return rootDirectory.Substring(0, rootDirectory.Length-1);
            }
            else
            {
                return rootDirectory;
            }
        }

        private string conditionNewDirectoryName(string currentName)
        {
            string localCurrentName = currentName;

            string[] conditions = new string[] { "-libx265-slow-qp21", " (1080p HD)", " (1080 HD)" };

            if (localCurrentName.Contains(conditions[0]))
            {
                localCurrentName = localCurrentName.Substring(0,localCurrentName.Length - conditions[0].Length);
            }

            //Console.WriteLine($"local current name after condition 0: { localCurrentName }");

            if (localCurrentName.Contains(conditions[1]))
            {
                localCurrentName = localCurrentName.Substring(0,localCurrentName.Length - conditions[1].Length);
                //Console.WriteLine($"local current name after condition 1 length - 0: { localCurrentName }");
            }
            else if (localCurrentName.Contains(conditions[1].Substring(0,conditions[1].Length-1)))
            {
                localCurrentName = localCurrentName.Substring(0, localCurrentName.Length - conditions[1].Length+1);
                //Console.WriteLine($"local current name after condition 1 length - 1: { localCurrentName }");
            }
            else if (localCurrentName.Contains(conditions[1].Substring(0,conditions[1].Length-2)))
            {
                localCurrentName = localCurrentName.Substring(0, localCurrentName.Length - conditions[1].Length+2);
                //Console.WriteLine($"local current name after condition 1 length - 2: { localCurrentName }");
            }
            else if (localCurrentName.Contains(conditions[1].Substring(0,conditions[1].Length-3)))
            {
                localCurrentName = localCurrentName.Substring(0, localCurrentName.Length - conditions[1].Length+3);
                //Console.WriteLine($"local current name after condition 1 length - 3: { localCurrentName }");
            }
            else if (localCurrentName.Contains(conditions[1].Substring(0,conditions[1].Length-4)))
            {
                localCurrentName = localCurrentName.Substring(0, localCurrentName.Length - conditions[1].Length+4);
                //Console.WriteLine($"local current name after condition 1 length - 4: { localCurrentName }");
            }
            else if (localCurrentName.Contains(conditions[1].Substring(0,conditions[1].Length-5)))
            {
                localCurrentName = localCurrentName.Substring(0, localCurrentName.Length - conditions[1].Length+5);
                //Console.WriteLine($"local current name after condition 1 length - 5: { localCurrentName }");
            }
            else if (localCurrentName.Contains(conditions[1].Substring(0,conditions[1].Length-6)))
            {
                localCurrentName = localCurrentName.Substring(0, localCurrentName.Length - conditions[1].Length+6);
                //Console.WriteLine($"local current name after condition 1 length - 6: { localCurrentName }");
            }
            else if (localCurrentName.Contains(conditions[1].Substring(0,conditions[1].Length-7)))
            {
                localCurrentName = localCurrentName.Substring(0, localCurrentName.Length - conditions[1].Length+7);
                //Console.WriteLine($"local current name after condition 1 length - 7: { localCurrentName }");
            }
            /*else if (localCurrentName.Contains(conditions[1].Substring(0,conditions[1].Length-8)))
            {
                localCurrentName = localCurrentName.Substring(0, localCurrentName.Length - conditions[1].Length-8);
            }*/

            //Console.WriteLine($"local current name after condition 1: { localCurrentName }");

            if (localCurrentName.Contains(conditions[2]))
            {
                localCurrentName = localCurrentName.Substring(0,localCurrentName.Length - conditions[2].Length);
            }
            else if (localCurrentName.Contains(conditions[2].Substring(0,conditions[2].Length-1)))
            {
                localCurrentName = localCurrentName.Substring(0, localCurrentName.Length - conditions[2].Length+1);
            }
            else if (localCurrentName.Contains(conditions[2].Substring(0,conditions[2].Length-2)))
            {
                localCurrentName = localCurrentName.Substring(0, localCurrentName.Length - conditions[2].Length+2);
            }
            else if (localCurrentName.Contains(conditions[2].Substring(0,conditions[2].Length-3)))
            {
                localCurrentName = localCurrentName.Substring(0, localCurrentName.Length - conditions[2].Length+3);
            }
            else if (localCurrentName.Contains(conditions[2].Substring(0,conditions[2].Length-4)))
            {
                localCurrentName = localCurrentName.Substring(0, localCurrentName.Length - conditions[2].Length+4);
            }

            //Console.WriteLine($"local current name after condition 2: { localCurrentName }");

            return localCurrentName;
        }
    }
}
