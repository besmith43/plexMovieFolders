package server

import (
	"go-plex-import-web/internal/view"
	"net/http"

	"github.com/labstack/echo/v4"
	"github.com/labstack/echo/v4/middleware"
)

func (s *Server) RegisterRoutes() http.Handler {
	e := echo.New()
	e.Use(middleware.Logger())
	e.Use(middleware.Recover())

	e.Use(middleware.CORSWithConfig(middleware.CORSConfig{
		AllowOrigins:     []string{"https://*", "http://*"},
		AllowMethods:     []string{"GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"},
		AllowHeaders:     []string{"Accept", "Authorization", "Content-Type", "X-CSRF-Token"},
		AllowCredentials: true,
		MaxAge:           300,
	}))

	e.Use(middleware.StaticWithConfig(middleware.StaticConfig{
		HTML5:      true,
		Root:       "assets", // because files are located in `web` directory in `webAssets` fs
		Filesystem: http.FS(view.Files),
	}))

	e.GET("/", view.GetCounterWebHandler)
	e.POST("/plus", view.PostPlusWebHandler)
	e.POST("/minus", view.PostMinusWebHandler)
	e.GET("/reset", view.GetResetWebHandler)

	return e
}
