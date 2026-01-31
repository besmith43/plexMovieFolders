package data

import (
	"os"
	"path/filepath"
)

func GetNewContent(source string) ([]string, error) {
	var directories []string

	err := filepath.Walk(source, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if info.IsDir() && path != source {
			// Extract just the directory name
			dirName := filepath.Base(path)
			directories = append(directories, dirName)
		}
		return nil
	})

	if err != nil {
		return nil, err
	}

	return directories, nil
}

func FindVideoFiles(dir string) ([]string, error) {
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

func GetCurrentTVSeries(rootTVDir string) ([]string, error) {
	var series []string

	err := filepath.Walk(rootTVDir, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if info.IsDir() && path != rootTVDir {
			// Extract just the directory name
			dirName := filepath.Base(path)
			series = append(series, dirName)
		}
		return nil
	})

	if err != nil {
		return nil, err
	}

	return series, nil
}
