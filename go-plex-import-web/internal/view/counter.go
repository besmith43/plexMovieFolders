package view

import (
	"fmt"
	"io/ioutil"
	"net/http"
	"strconv"
	"strings"

	"github.com/labstack/echo/v4"
)

func GetCounterWebHandler(c echo.Context) error {
	var count int = 0

	return render(c, 200, Counter(count))
}

func PostPlusWebHandler(c echo.Context) error {
	body, err := ioutil.ReadAll(c.Request().Body)
	if err != nil {
		return c.String(http.StatusBadRequest, "bad request")
	}
	defer c.Request().Body.Close()

	fmt.Println("Request Body:", string(body))

	tmp := strings.Split(string(body), "=")

	count, err := strconv.Atoi(tmp[1])
	if err != nil {
		return c.String(http.StatusBadRequest, "bad request")
	}

	count += 1

	return render(c, 200, Plus(count))
}

func PostMinusWebHandler(c echo.Context) error {
	body, err := ioutil.ReadAll(c.Request().Body)
	if err != nil {
		return c.String(http.StatusBadRequest, "bad request")
	}
	defer c.Request().Body.Close()

	fmt.Println("Request Body:", string(body))

	tmp := strings.Split(string(body), "=")

	count, err := strconv.Atoi(tmp[1])
	if err != nil {
		return c.String(http.StatusBadRequest, "bad request")
	}

	count -= 1

	return render(c, 200, Minus(count))
}

func GetResetWebHandler(c echo.Context) error {
	return render(c, 200, Reset())
}
