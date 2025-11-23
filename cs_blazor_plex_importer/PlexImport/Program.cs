using PlexImport;
using PlexImport.Components;
using PlexImport.DTO;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddRazorComponents();

builder.Services.AddCors(options =>
{
    options.AddPolicy("Cors",
        builder =>
        {
            builder.AllowAnyOrigin()
                .AllowAnyMethod()
                .AllowAnyHeader();
        });
});

var app = builder.Build();

app.UseCors("Cors");

SharedData.src = app.Configuration.GetValue<string>("SRC");
if (String.IsNullOrEmpty(SharedData.src))
{
    throw new Exception("Source Path is missing");
}
// else
// {
//     Console.WriteLine($"Source Path: {SharedData.src}");
// }

SharedData.movie = app.Configuration.GetValue<string>("Movie");
if (String.IsNullOrEmpty(SharedData.movie))
{
    throw new Exception("Movie Path is missing");
}
// else
// {
//     Console.WriteLine($"Movie Path: {SharedData.movie}");
// }

SharedData.tvshows = app.Configuration.GetValue<string>("TVShow");
if (String.IsNullOrEmpty(SharedData.tvshows))
{
    throw new Exception("TV Show Path is missing");
}
// else
// {
//     Console.WriteLine($"TV Show Path: {SharedData.tvshows}");
// }


if (!Directory.Exists(SharedData.tvshows))
{
    throw new Exception("TV Show Path doesn't exist");
}

string[] immediateSubdirectories = Directory.GetDirectories(SharedData.tvshows);

SharedData.tvshowList = immediateSubdirectories.ToList<string>();


// Configure the HTTP request pipeline.
if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Error", createScopeForErrors: true);
    // The default HSTS value is 30 days. You may want to change this for production scenarios, see https://aka.ms/aspnetcore-hsts.
    app.UseHsts();
}
app.UseStatusCodePagesWithReExecute("/not-found", createScopeForStatusCodePages: true);
app.UseHttpsRedirection();

app.UseAntiforgery();

app.MapStaticAssets();
app.MapRazorComponents<App>();


app.MapPost("/api/movie", (Movie movie) =>
{
    Console.WriteLine($"uri: {movie.uri}\ntitle: {movie.title}\nyear: {movie.year}");

    return Results.Accepted();
});


app.MapPost("/api/tvshow", (TVShow tv) =>
{
    Console.WriteLine($"uri: {tv.uri}\ntitle: {tv.title}\nseason: {tv.season}\nepisode: {tv.episode}");

    return Results.Accepted();
});


app.Run();
