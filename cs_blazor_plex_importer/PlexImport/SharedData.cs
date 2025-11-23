namespace PlexImport;


public static class SharedData
{
#if DEBUG
	public static bool blnDebug = true;
#else
    public static bool blnDebug = false;
#endif

	public static string src = String.Empty;
	public static string movie = String.Empty;
	public static string tvshows = String.Empty;
}




