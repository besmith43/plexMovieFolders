package main

import (
	"bufio"
	"flag"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"time"

	"github.com/fatih/color"
	fuzzyfinder "github.com/ktr0731/go-fuzzyfinder"
)

// var searchDir string = "/Volumes/LightSpeed/containers/sabnzbd/config/Downloads/complete"
var searchDir string = "/Volumes/LightSpeed/containers/sabnzbd/config/Downloads/complete/new-uploads"
var rootDest string = "/Volumes/Content_Vault/Plex"

func main() {
	sourcePtr := flag.String("source", "", "source search directory")
	destPtr := flag.String("dest", "", "destination directory")
	flag.Parse()

	if *sourcePtr != "" {
		searchDir = *sourcePtr
	}

	err := validateDirectory(searchDir)
	if err != nil {
		panic(err)
	}

	if *destPtr != "" {
		rootDest = *destPtr
	}

	err = validateDirectory(rootDest)
	if err != nil {
		panic(err)
	}

	new_content, err := scanSourceDirectory(searchDir)
	if err != nil {
		panic(err)
	}

	for _, dir := range new_content {
		err := processDirectory(dir)
		if err != nil {
			fmt.Printf("Error processing directory %s: %v\n", dir, err)
		} else {
			ClearScreen()
		}
	}
}

func processDirectory(dir string) error {

	choice, err := getContentType(dir)
	if err != nil {
		return err
	}

	if choice == "quit" {
		fmt.Println("Quitting program.")
		os.Exit(0)
	} else if choice == "skip" {
		fmt.Printf("Skipping directory %s\n", dir)
		return nil
	} else if choice == "movie" {
		return processMovie(dir)
	} else if choice == "tv show" {
		return processTVShow(dir)
	}

	return nil
}

func processMovie(dir string) error {
	// Implement movie processing logic here
	fmt.Println("Processing movie in directory:", dir)

	videoFiles, err := findVideoFiles(dir)
	if err != nil {
		return err
	}

	var selectedFile string
	if len(videoFiles) == 0 {
		fmt.Println("No video files found in directory:", dir)
		fmt.Println("Will continue in 2 seconds...")
		// sleep for 2 seconds to let user read message
		time.Sleep(2 * time.Second)
		return nil
	} else if len(videoFiles) > 1 {
		fmt.Println("Multiple video files found in directory:", dir)
		selectedFile, err = selectVideoFile(videoFiles)
	} else {
		selectedFile = videoFiles[0]
		fmt.Print("Single video file found: ")
		color.Green(filepath.Base(selectedFile))
	}

	fmt.Println("What is the movie title?")
	var movieTitle string

	scanner := bufio.NewScanner(os.Stdin)
	if scanner.Scan() {
		movieTitle = scanner.Text()
	}
	if err := scanner.Err(); err != nil {
		return err
	}

	fmt.Println("What year was the movie released?")
	var movieYear int
	_, err = fmt.Scanf("%d", &movieYear)
	if err != nil {
		return err
	}

	if movieYear < 1900 || movieYear > 2100 {
		return fmt.Errorf("invalid year: %d", movieYear)
	}

	movieTitle = CleanStringForFilename(movieTitle)

	destDir := filepath.Join(rootDest, "Movies", fmt.Sprintf("%s (%d)", movieTitle, movieYear))

	destFile := filepath.Join(destDir, fmt.Sprintf("%s (%d)%s", movieTitle, movieYear, filepath.Ext(selectedFile)))

	confirm, err := YesOrNoPrompt("Is this path correct? ", destFile)
	if err != nil {
		return err
	}
	if !confirm {
		fmt.Println("Aborting movie processing.")
		return processMovie(dir)
	}

	err = os.MkdirAll(destDir, os.ModePerm)
	if err != nil {
		return fmt.Errorf("failed to create destination directory: %w", err)
	}

	err = moveFileWithProgress(selectedFile, destFile)
	if err != nil {
		return fmt.Errorf("failed to move file: %w", err)
	}

	fmt.Println("\nMovie processed successfully:", destFile)

	// clean up empty directory
	err = os.Remove(dir)
	if err != nil {
		fmt.Printf("Warning: failed to remove directory %s: %v\n", dir, err)
	}

	return nil
}

func YesOrNoPrompt(prompt string, file string) (bool, error) {
	for {
		color.Green(file)
		fmt.Printf("%s (y/n): ", prompt)
		var response string
		_, err := fmt.Scanf("%s", &response)
		if err != nil {
			return false, err
		}

		switch response {
		case "y", "Y":
			return true, nil
		case "n", "N":
			return false, nil
		default:
			fmt.Println("invalid input, please enter 'y' or 'n'")
		}
	}
}

func selectVideoFile(videoFiles []string) (string, error) {
	fmt.Println("Select the video file to process:")
	for i, file := range videoFiles {
		fmt.Printf("%d. %s\n", i+1, filepath.Base(file))
	}
	fmt.Print("Enter your choice (number): ")

	var choice int
	_, err := fmt.Scanf("%d", &choice)
	if err != nil {
		return "", err
	}

	if choice < 1 || choice > len(videoFiles) {
		return "", fmt.Errorf("invalid choice")
	}
	return videoFiles[choice-1], nil
}

func processTVShow(dir string) error {
	// Implement TV show processing logic here
	fmt.Println("Processing TV show in directory:", dir)

	videoFiles, err := findVideoFiles(dir)
	if err != nil {
		return err
	}

	var selectedFile string
	if len(videoFiles) == 0 {
		fmt.Println("No video files found in directory:", dir)
		fmt.Println("Will continue in 2 seconds...")
		// sleep for 2 seconds to let user read message
		time.Sleep(2 * time.Second)
		return nil
	} else if len(videoFiles) > 1 {
		fmt.Println("Multiple video files found in directory:", dir)
		selectedFile, err = selectVideoFile(videoFiles)
	} else {
		selectedFile = videoFiles[0]
		fmt.Print("Single video file found: ")
		color.Green(filepath.Base(selectedFile))
	}

	color.Green(selectedFile)

	shows, err := getTVShowsList()
	if err != nil {
		return err
	}

	var seriesName string
	seriesName, err = FuzzySearchTVShow(shows)
	if err != nil {
		return err
	}

	if seriesName == "Add New Show" {
		fmt.Println("Enter the name of the new TV show:")
		scanner := bufio.NewScanner(os.Stdin)
		if scanner.Scan() {
			seriesName = scanner.Text()
		}
		if err := scanner.Err(); err != nil {
			return err
		}
	}

	fmt.Println("What is the Season Number? ")
	var seasonNumber int
	_, err = fmt.Scanf("%d", &seasonNumber)
	if err != nil {
		return err
	}

	fmt.Println("What is the Episode Number? ")
	var episodeNumber int
	_, err = fmt.Scanf("%d", &episodeNumber)
	if err != nil {
		return err
	}

	seriesName = CleanStringForFilename(seriesName)

	destDir := filepath.Join(rootDest, "TV Shows", seriesName, fmt.Sprintf("Season %02d", seasonNumber))
	destFile := filepath.Join(destDir, fmt.Sprintf("%s - s%02de%02d%s", seriesName, seasonNumber, episodeNumber, filepath.Ext(selectedFile)))

	confirm, err := YesOrNoPrompt("Is this path correct? ", destFile)
	if err != nil {
		return err
	}
	if !confirm {
		fmt.Println("Aborting tv show processing.")
		return processTVShow(dir)
	}

	err = os.MkdirAll(destDir, os.ModePerm)
	if err != nil {
		return fmt.Errorf("failed to create destination directory: %w", err)
	}

	err = moveFileWithProgress(selectedFile, destFile)
	if err != nil {
		return fmt.Errorf("failed to move file: %w", err)
	}

	fmt.Println("\nTV Show processed successfully:", destFile)

	// clean up empty directory
	err = os.Remove(dir)
	if err != nil {
		fmt.Printf("Warning: failed to remove directory %s: %v\n", dir, err)
	}

	return nil
}

func CleanStringForFilename(input string) string {
	invalidChars := []rune{'<', '>', ':', '"', '/', '\\', '|', '?', '*', '\n', '\r', '\t', '\''}
	cleaned := []rune{}
	for _, r := range input {
		invalid := false
		for _, invalidChar := range invalidChars {
			if r == invalidChar {
				invalid = true
				break
			}
		}
		if !invalid {
			cleaned = append(cleaned, r)
		}
	}
	return string(cleaned)
}

func FuzzySearchTVShow(shows []string) (string, error) {

	fmt.Println("Select the TV show to add this episode to:")

	items := []struct {
		ID   string
		Name string
	}{
		{"1", "Add New Show"},
	}

	for i, show := range shows {
		items = append(items, struct {
			ID   string
			Name string
		}{
			ID:   fmt.Sprintf("%d", i+2),
			Name: show,
		})
	}

	idx, err := fuzzyfinder.Find(
		items,
		func(i int) string {
			return items[i].Name
		},
		fuzzyfinder.WithPromptString("Search TV Shows> "),
	)
	if err != nil {
		return "", err
	}

	selectedShow := items[idx].Name
	fmt.Printf("You selected: %s\n", selectedShow)
	return selectedShow, nil
}

func getTVShowsList() ([]string, error) {
	tvShowsDir := filepath.Join(rootDest, "TV Shows")

	entries, err := os.ReadDir(tvShowsDir)
	if err != nil {
		return nil, fmt.Errorf("failed to read TV Shows directory: %w", err)
	}

	var shows []string
	for _, entry := range entries {
		if entry.IsDir() {
			shows = append(shows, entry.Name())
		}
	}

	return shows, nil
}

func findVideoFiles(dir string) ([]string, error) {
	var videoFiles []string
	videoExtensions := map[string]bool{
		".mp4": true,
		".mkv": true,
		".avi": true,
		".mov": true,
	}

	err := filepath.Walk(dir, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if !info.IsDir() {
			ext := filepath.Ext(info.Name())
			if videoExtensions[ext] {
				videoFiles = append(videoFiles, path)
			}
		}
		return nil
	})

	if err != nil {
		return nil, err
	}

	return videoFiles, nil
}

type ContentType int

const (
	Movie ContentType = iota
	TVShow
	Skip
	Quit
)

func getContentType(dir string) (string, error) {
	for {
		items := []struct {
			ID   string
			Name string
		}{
			{"1", "Movie"},
			{"2", "TV Show"},
			{"3", "Skip"},
			{"4", "Quit"},
		}

		idx, err := fuzzyfinder.Find(
			items,
			func(i int) string {
				return items[i].Name
			},
			fuzzyfinder.WithPromptString(fmt.Sprintf("What kind of content is %s > ", dir)),
		)
		if err != nil {
			return "", err
		}

		choice := items[idx].Name
		fmt.Printf("You selected: %s\n", choice)
		return strings.ToLower(choice), nil
	}
}

func validateDirectory(path string) error {
	info, err := os.Stat(path)
	if err != nil {
		return fmt.Errorf("path does not exist: %w", err)
	}
	if !info.IsDir() {
		return fmt.Errorf("path is not a directory: %s", path)
	}
	return nil
}

func scanSourceDirectory(source string) ([]string, error) {
	var directories []string

	err := filepath.Walk(source, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if info.IsDir() && path != source {
			directories = append(directories, path)
		}
		return nil
	})

	if err != nil {
		return nil, err
	}

	return directories, nil
}

func moveFileWithProgress(source, destination string) error {
	// Open source file
	srcFile, err := os.Open(source)
	if err != nil {
		return fmt.Errorf("failed to open source file: %w", err)
	}
	defer srcFile.Close()

	// Get file info for size
	fileInfo, err := srcFile.Stat()
	if err != nil {
		return fmt.Errorf("failed to get file info: %w", err)
	}
	fileSize := fileInfo.Size()

	// Create destination file
	destFile, err := os.Create(destination)
	if err != nil {
		return fmt.Errorf("failed to create destination file: %w", err)
	}
	defer destFile.Close()

	// Copy with progress
	fmt.Printf("Moving %s to %s\n", filepath.Base(source), filepath.Base(destination))
	fmt.Printf("Size: %.2f MB\n\n", float64(fileSize)/(1024*1024))

	reader := &ProgressReader{
		Reader:   srcFile,
		Total:    fileSize,
		Progress: 0,
	}

	_, err = io.Copy(destFile, reader)
	if err != nil {
		return fmt.Errorf("failed to copy file: %w", err)
	}

	// Remove source file after successful copy
	if err := os.Remove(source); err != nil {
		return fmt.Errorf("failed to remove source file: %w", err)
	}

	return nil
}

type ProgressReader struct {
	Reader   io.Reader
	Total    int64
	Progress int64
}

func (pr *ProgressReader) Read(p []byte) (int, error) {
	n, err := pr.Reader.Read(p)
	pr.Progress += int64(n)

	// Update progress bar
	pr.printProgress()

	return n, err
}

func (pr *ProgressReader) printProgress() {
	percent := float64(pr.Progress) / float64(pr.Total) * 100
	barWidth := 50
	filled := int(percent / 100 * float64(barWidth))

	bar := ""
	for i := 0; i < barWidth; i++ {
		if i < filled {
			bar += "█"
		} else {
			bar += "░"
		}
	}

	fmt.Printf("\r[%s] %.1f%% (%.2f/%.2f MB)",
		bar,
		percent,
		float64(pr.Progress)/(1024*1024),
		float64(pr.Total)/(1024*1024))
}

// ClearScreen clears the terminal screen based on the operating system.
func ClearScreen() {
	var cmd *exec.Cmd
	if runtime.GOOS == "windows" {
		cmd = exec.Command("cmd", "/c", "cls")
	} else { // Linux, macOS, etc.
		cmd = exec.Command("clear")
	}
	cmd.Stdout = os.Stdout
	cmd.Run()
}
