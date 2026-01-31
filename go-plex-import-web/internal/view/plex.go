package view

import (
	"fmt"
	"go-plex-import-web/internal/data"
	"net/http"

	"github.com/labstack/echo/v4"
)

var root_dir string = "test_root_dir"
var dest_dir string = "test_dest_dir"

func GetPlexHandler(c echo.Context) error {
	new_content, err := data.GetNewContent(root_dir)
	if err != nil {
		return c.String(http.StatusInternalServerError, fmt.Sprintf("something went wrong\nerror: %s", err.Error()))
	}

	return render(c, 200, Plex(new_content))
}

func GetProcessingDialogHandler(c echo.Context) error {
	// get directory picked from the request context
	name := c.Param("name")

	// get the video files found in that directory
	files, err := data.FindVideoFiles(fmt.Sprintf("%s/%s", root_dir, name))
	if err != nil {
		return c.String(http.StatusInternalServerError, fmt.Sprintf("something went wrong\nerror: %s", err.Error()))
	}

	var selectedFile string
	if len(files) == 0 {
		// return c.String(http.StatusNoContent, "nothing found")
		// fmt.Println("GetProcessingDialogHandler: no content found")
		return c.NoContent(http.StatusNoContent) // or c.NoContent(204)
	} else if len(files) == 1 {
		selectedFile = files[0]
	} else {
		// eventually this will return the multiple files
		return c.String(http.StatusConflict, "more than 1 video file found")
	}

	series, err := data.GetCurrentTVSeries(fmt.Sprintf("%s/%s", dest_dir, "TV Shows"))
	if err != nil {
		return c.String(http.StatusInternalServerError, fmt.Sprintf("Error: %s", err.Error()))
	}

	// pass it to the process dialog template
	return render(c, 200, ProcessingDialog(selectedFile, series))
}

func PostProcessingDialogHandler(c echo.Context) error {
	choice := c.FormValue("contentTypeSelect")
	fmt.Printf("Choice: %s", choice)
	return nil
}
