package main

import (
	"log"
	"net/http"

	"git.toptal.com/backend/router"
)

func main() {

	router := router.New()
	log.Fatal(http.ListenAndServe(":8080", router))
}
