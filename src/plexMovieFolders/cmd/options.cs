using System;
using System.Text;
using System.Reflection;

#nullable enable

namespace plexMovieFolders.cmd
{
    public class Options
    {
        public string directory { get; set; }
        public bool parallel { get; set; }
        public bool helpFlag { get; set; }
        public string helpText { get; set; }
        public bool versionFlag { get; set; }
        public string versionText { get; set; }
        public bool verbose { get; set; }

        public Options()
        {
            helpFlag = false;
            versionFlag = false;
            versionText = $"plexMovieFolders { Assembly.GetEntryAssembly().GetCustomAttribute<AssemblyFileVersionAttribute>().Version }";
            verbose = false;
            StringBuilder helpTextBuilder = new StringBuilder();
            helpTextBuilder.AppendLine($"{ versionText }");
            helpTextBuilder.AppendLine("Usage:  plexMovieFolders [OPTION]  ");
            helpTextBuilder.AppendLine("");
            helpTextBuilder.AppendLine("    -d | --directory [path] Set the path to operate on");
            helpTextBuilder.AppendLine("    -p | --parallel         Set the parallel flag to true");
            helpTextBuilder.AppendLine("    -V | --verbose          Set verbose mode");
            helpTextBuilder.AppendLine("    -v | --version          Display version message");
            helpTextBuilder.AppendLine("    -h | --help             Display this help message");
            helpTextBuilder.AppendLine("");
            helpText = helpTextBuilder.ToString();
        }
    }
}