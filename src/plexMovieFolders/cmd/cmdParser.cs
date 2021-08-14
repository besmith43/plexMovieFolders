namespace plexMovieFolders.cmd
{
    public class cmdParser
    {
        private string[] _args;
        private Options opts;

        public cmdParser(string[] Args)
        {
            _args = Args;
        }

        public Options Parse()
        {
            opts = new();

            if (_args != null)
            {
                for (int i = 0; i < _args.Length; i++)
                {
                    if (_args[i] == "-h" || _args[i] == "--help")
                    {
                        opts.helpFlag = true;
                    }
                    else if (_args[i] == "-V" || _args[i] == "--version")
                    {
                        opts.versionFlag = true;
                    }
                    else if (_args[i] == "-v" || _args[i] == "--verbose")
                    {
                        opts.verbose = true;
                    }
                    else if (_args[i] == "-p" || _args[i] == "--parallel")
                    {
                        opts.parallel = true;
                    }
                    else if (_args[i] == "-d" || _args[i] == "--directory")
                    {
                        opts.directory = _args[i+1];
                        i++;
                    }
                    else
                    {
                        // place any unnamed or positional value checks
                    }
                }
            }

            return opts;
        }
    }
}