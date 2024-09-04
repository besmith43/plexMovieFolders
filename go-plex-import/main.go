package main

import (
	"flag"
	"fmt"
	"os"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/bitfield/script"
	"github.com/charmbracelet/huh"
	"github.com/charmbracelet/huh/spinner"
	// . "github.com/tylerwince/godbg"
)

var dryRun bool = false
var searchDir string = "/volume1/docker/sabnzbd/Downloads/complete"
var rootDest string = "/volume1/Plex"
var finalSrc string
var finalDest string
var finalDestDir string

func main() {
	// var searchDir string

	// Dbg(os.Args)
	// Dbg(len(os.Args))

	dryRunPtr := flag.Bool("dryrun", false, "dry run disables the move step")
	sourcePtr := flag.String("source", "", "source search directory")
	destPtr := flag.String("dest", "", "destination directory")
	flag.Parse()

	dryRun = *dryRunPtr

	// Dbg(dryRun)

	/*
		if len(os.Args) == 1 {
			wd, err := os.Getwd()
			if err != nil {
				fmt.Println(err)
				os.Exit(1)
			}
			searchDir = wd
		} else {
			searchDir = os.Args[len(os.Args)-1]
		}
	*/

	if *sourcePtr != "" {
		searchDir = *sourcePtr
	}

	// Dbg(searchDir)

	if *destPtr != "" {
		rootDest = *destPtr
	}

	selections, err := Select(searchDir)
	if err != nil {
		fmt.Println(err)
	}

	err = Process(selections)
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}

func Select(rootDir string) ([]string, error) {

	var selections []string

	// dirs, err := getDirectionNames(rootDir)
	files, err := getFileOptions(rootDir)

	form := huh.NewForm(
		huh.NewGroup(
			huh.NewMultiSelect[string]().
				Options(files...).
				Title("Select Content").
				Value(&selections),
		),
	)

	err = form.Run()
	if err != nil {
		return nil, err
	}

	return selections, nil
}

func getFileOptions(root string) ([]huh.Option[string], error) {

	regex := regexp.MustCompile(`^.*\.(mp4|mkv)$`)

	rawfiles, err := script.FindFiles(root).MatchRegexp(regex).String()
	if err != nil {
		return nil, err
	}

	files := strings.Split(rawfiles, "\n")

	var fileOptions []huh.Option[string]

	for _, file := range files {
		fileOptions = append(fileOptions, huh.NewOption(file, file))
	}

	return fileOptions, nil
}

func getDirectionNames(root string) ([]huh.Option[string], error) {

	dirs, err := os.ReadDir(root)
	if err != nil {
		return nil, err
	}

	// var dirsStrings []string

	var dirOptions []huh.Option[string]

	for _, dir := range dirs {
		dirName := dir.Name()
		dirOptions = append(dirOptions, huh.NewOption(dirName, dirName))
		// dirsStrings = append(dirsStrings, dir.Name())
	}

	return dirOptions, nil
}

func Process(selections []string) error {
	for _, selection := range selections {
		err := process(selection)
		if err != nil {
			return err
		}
	}

	return nil
}

func process(selection string) error {
	fmt.Println(selection)

	option, err := TVorMovie(selection)
	if err != nil {
		return err
	}

	// fmt.Printf("%s is a %s\n", selection, option)

	if option == "movie" {
		err = processMovie(selection)
		if err != nil {
			return err
		}
	} else if option == "tv" {
		err = processTVShow(selection)
		if err != nil {
			return err
		}
	} else {
		panic("just how..?")
	}

	return nil
}

func TVorMovie(selection string) (string, error) {
	var option string
	form := huh.NewForm(
		huh.NewGroup(
			huh.NewSelect[string]().
				Title(fmt.Sprintf("Is %s a Movie or TV Show?", selection)).
				Options(
					huh.NewOption("Movie", "movie"),
					huh.NewOption("TV Show", "tv"),
				).
				Value(&option),
		),
	)

	err := form.Run()
	if err != nil {
		return "", err
	}

	return option, nil
}

func processMovie(selection string) error {

	fmt.Printf("%s is a Movie\n", selection)

	var title string
	var yearString string
	var year int

	form := huh.NewForm(
		huh.NewGroup(
			huh.NewInput().
				Title(fmt.Sprintf("What is %s's title?", selection)).
				Value(&title),
		),

		huh.NewGroup(
			huh.NewInput().
				Title(fmt.Sprintf("What year was  %s released?", selection)).
				Value(&yearString).
				Validate(func(ans string) error {
					_, err := strconv.Atoi(ans)
					if err != nil {
						return err
					}
					// Dbg(num)
					return nil
				}),
		),
	)

	err := form.Run()
	if err != nil {
		return err
	}

	year, err = strconv.Atoi(yearString)
	if err != nil {
		return err
	}

	// Dbg(title)
	// Dbg(year)

	fileSplit := strings.Split(selection, ".")
	fileExtension := fileSplit[len(fileSplit)-1]

	movieDir := fmt.Sprintf("%s (%d)", title, year)
	movieFileName := fmt.Sprintf("%s (%d).%s", title, year, fileExtension)

	destinationDir := fmt.Sprintf("%s/Movies/%s", rootDest, movieDir)

	destination := fmt.Sprintf("%s/Movies/%s/%s", rootDest, movieDir, movieFileName)

	// Dbg(destination)

	var confirmationResponse bool

	form = huh.NewForm(
		huh.NewGroup(
			huh.NewConfirm().
				Title(fmt.Sprintf("Does this path look right?\n%s", destination)).
				Value(&confirmationResponse),
		),
	)

	err = form.Run()
	if err != nil {
		return err
	}

	if !confirmationResponse {
		fmt.Println("user cancelled")
		return nil
	}

	if dryRun {
		fmt.Println("Dry Run")

		action := func() {
			time.Sleep(2 * time.Second)
		}

		_ = spinner.New().
			Title("Moving File to New Home...").
			Action(action).
			Run()

		return nil
	}

	finalSrc = selection
	finalDest = destination
	finalDestDir = destinationDir

	copyAction := func() {
		// os.MkdirAll(finalDestDir, 0777)
		os.MkdirAll(finalDestDir, os.ModePerm)
		err = os.Rename(finalSrc, finalDest)
		if err != nil {
			fmt.Println("Error!!!!")
			// fmt.Println(err)
			fmt.Println(err.Error())
			// fmt.Printf("Source: %s\n", finalSrc)
			// fmt.Printf("Destination: %s\n", finalDest)
			// fmt.Printf("Destination Directory: %s\n", finalDestDir)
			fmt.Println("attempting bitfield script")
			err = script.Exec(fmt.Sprintf("mv %s %s", finalSrc, finalDest)).Close()
			if err != nil {
				fmt.Println(err)
			}
		}
	}

	err = spinner.New().
		Title("Moving File to New Home...").
		Action(copyAction).
		Run()

	fmt.Println("Done")

	return nil
}

func processTVShow(selection string) error {

	fmt.Printf("%s is a TV Show\n", selection)

	var seriesTitle string
	var seasonString string
	var season int
	var episodeString string
	var episode int

	/*
		currentSeriesTitles, err := script.ListFiles(fmt.Sprintf("%s/TV Shows/", rootDest)).String()
		if err != nil {
			return err
		}
	*/

	currentSeriesDirOptions, err := getDirectionNames(fmt.Sprintf("%s/TV Shows/", rootDest))
	if err != nil {
		return err
	}

	currentSeriesDirOptions = append(currentSeriesDirOptions, huh.NewOption("New Series", "New Series"))

	form := huh.NewForm(
		huh.NewGroup(
			huh.NewSelect[string]().
				Options(currentSeriesDirOptions...).
				Title(fmt.Sprintf("What is %s's series name?", selection)).
				Value(&seriesTitle),
		),

		huh.NewGroup(
			huh.NewInput().
				Title(fmt.Sprintf("%s - Season Number: ", selection)).
				Value(&seasonString).
				Validate(func(ans string) error {
					_, err := strconv.Atoi(ans)
					if err != nil {
						return err
					}
					// Dbg(num)
					return nil
				}),
		),

		huh.NewGroup(
			huh.NewInput().
				Title(fmt.Sprintf("%s - Episode Number: ", selection)).
				Value(&episodeString).
				Validate(func(ans string) error {
					_, err := strconv.Atoi(ans)
					if err != nil {
						return err
					}
					// Dbg(num)
					return nil
				}),
		),
	)

	err = form.Run()
	if err != nil {
		return err
	}

	if seriesTitle == "New Series" {
		form := huh.NewForm(
			huh.NewGroup(
				huh.NewInput().
					Title(fmt.Sprintf("what is the series called for %s?", selection)).
					Value(&seriesTitle),
			),
		)

		err = form.Run()
		if err != nil {
			return err
		}
	}

	season, err = strconv.Atoi(seasonString)
	if err != nil {
		return err
	}

	episode, err = strconv.Atoi(episodeString)
	if err != nil {
		return err
	}

	// Dbg(seriesTitle)
	// Dbg(season)
	// Dbg(episode)

	if season < 10 {
		seasonString = fmt.Sprintf("0%d", season)
	}

	if episode < 10 {
		episodeString = fmt.Sprintf("0%d", episode)
	}

	fileSplit := strings.Split(selection, ".")
	fileExtension := fileSplit[len(fileSplit)-1]

	episodeFileName := fmt.Sprintf("%s-s%se%s.%s", seriesTitle, seasonString, episodeString, fileExtension)

	destinationDir := fmt.Sprintf("%s/TV Shows/%s/Season %s", rootDest, seriesTitle, seasonString)

	destination := fmt.Sprintf("%s/TV Shows/%s/Season %s/%s", rootDest, seriesTitle, seasonString, episodeFileName)

	// Dbg(destination)

	var confirmationResponse bool

	form = huh.NewForm(
		huh.NewGroup(
			huh.NewConfirm().
				Title(fmt.Sprintf("Does this path look right?\n%s", destination)).
				Value(&confirmationResponse),
		),
	)

	err = form.Run()
	if err != nil {
		return err
	}

	if !confirmationResponse {
		fmt.Println("user cancelled")
		return nil
	}

	if dryRun {
		fmt.Println("Dry Run")

		action := func() {
			time.Sleep(2 * time.Second)
		}

		_ = spinner.New().
			Title("Moving File to New Home...").
			Action(action).
			Run()

		return nil
	}

	// err = os.Rename(selection, destination)

	finalSrc = selection
	finalDest = destination
	finalDestDir = destinationDir

	copyAction := func() {
		// os.MkdirAll(finalDestDir, 0777)
		os.MkdirAll(finalDestDir, os.ModePerm) // os.ModePerm is basically just 0777 anyways
		err = os.Rename(finalSrc, finalDest)
		if err != nil {
			fmt.Println("Error!!!!")
			// fmt.Println(err)
			fmt.Println(err.Error())
			// fmt.Printf("Source: %s\n", finalSrc)
			// fmt.Printf("Destination: %s\n", finalDest)
			// fmt.Printf("Destination Directory: %s\n", finalDestDir)
			fmt.Println("attempting bitfield script")
			err = script.Exec(fmt.Sprintf("mv %s %s", finalSrc, finalDest)).Close()
			if err != nil {
				fmt.Println(err)
			}
		}
	}

	err = spinner.New().
		Title("Moving File to New Home...").
		Action(copyAction).
		Run()

	fmt.Println("Done")

	return nil
}
