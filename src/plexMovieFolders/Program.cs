using System;
using System.IO;
using plexMovieFolders.cmd;
using plexMovieFolders.Class;

namespace plexMovieFolders
{
    class Program
    {
        public static Options cmdFlags;
        static void Main(string[] args)
        {
            cmdParser cmdP = new(args);

            cmdFlags = cmdP.Parse();  

            if (cmdFlags.versionFlag)
            {
                Console.WriteLine(cmdFlags.versionText);
                return;
            }

            if (cmdFlags.helpFlag)
            {
                Console.WriteLine(cmdFlags.helpText);
                return;
            }

            if (!Directory.Exists(cmdFlags.directory))
            {
                Console.WriteLine($"The directory passed in does not exist\n{ cmdFlags.directory }");
                return;
            }

            Run();
        }

        public static void Run()
        {
            Class1 classLib = new Class1();

            if (cmdFlags.parallel)
            {
                classLib.RunParallel(cmdFlags.directory);
            }
            else
            {
                classLib.RunSerial(cmdFlags.directory);
            }
        }
    }
}
