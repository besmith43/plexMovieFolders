#addin nuget:?package=SSH.NET&version=2024.2.0
#addin nuget:?package=BouncyCastle.Cryptography&version=2.5.0

using Renci.SshNet;

var target = Argument("target", "Default");
var configuration = Argument("configuration", "Debug");

var project = "plex_importer";
var csproj = $"./{project}/{project}.csproj";
var testCsproj = $"./{project}.Test/{project}.Test.csproj";
var buildDir = "./.build";
var publishDir = "./.publish";


Task("Clean")
	.Does(() =>
{
	CleanDirectory(buildDir);

	DotNetClean(csproj, new DotNetCleanSettings
	{
		Configuration = configuration,
		OutputDirectory = buildDir
	});

	CleanDirectory(publishDir);

	DotNetClean(csproj, new DotNetCleanSettings
	{
		Configuration = "Release",
		OutputDirectory = publishDir
	});
});


Task("Test")
        .Does(() =>
{
    DotNetTest(testCsproj, new DotNetTestSettings
    {
        Configuration = configuration
    });
});


Task("Build")
        .Does(() =>
{
        DotNetBuild(csproj, new DotNetBuildSettings
        {
                Configuration = configuration,
                NoRestore = false,
                NoLogo = true,
                Verbosity = DotNetVerbosity.Minimal,
                OutputDirectory = buildDir
        });
});


Task("Run")
        .Does(() =>
{
        DotNetExecute($"{buildDir}/{project}.dll");
});


Task("Publish")
	.IsDependentOn("Clean")
	.Does(() =>
{
	DotNetPublish(csproj, new DotNetPublishSettings
	{
		Configuration = "Release",
		NoRestore = false,
		NoLogo = true,
		Runtime = "linux-x64",
		SelfContained = true,
		PublishSingleFile = true,
		Verbosity = DotNetVerbosity.Minimal,
		OutputDirectory = publishDir
	});
});


Task("Deploy")
    .IsDependentOn("Publish")
    .Does(() =>
{
    var host = "100.122.164.20";
    // var host = "nas";
    var user = "besmith";
    var sshKey = "/Users/besmith/.ssh/id_rsa";
    var port = 22;
    var binary = $"{publishDir}/{project}";
    Information($"Binary Value: {binary}");

    var dest = $"/var/services/homes/besmith/bin/{project}";

    if (!System.IO.File.Exists(binary))
    {
        Error("Published Executable doesn't Exist");
        System.Environment.Exit(1);
    }


    using (var client = new ScpClient(host, port, user, new PrivateKeyFile(sshKey)))
    {
        client.Connect();

        try
        {
            client.Upload(new FileInfo(binary), dest);
        } catch (Exception ex) {
            Information($"SSH Upload Failed with the following error message {Environment.NewLine}{ex.Message}");
            System.Environment.Exit(1);
        }

        Information("SSH Upload Succeeded");
    }
});

Task("Default")
	.IsDependentOn("Test");


RunTarget(target); // this is going to run the default task if you don't pass in a --target argument



