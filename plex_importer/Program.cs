using plex_importer;

try
{
    var search = new Search(args[0]);
    search.Run();
}
catch (Exception ex)
{
    Console.WriteLine(ex.Message);
}

