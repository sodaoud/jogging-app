package router

import (
	"github.com/gorilla/mux"
	"github.com/julienschmidt/httprouter"
)

// New creates a new httprouter and add the handlers to it
func New() *mux.Router {
	httprouter.New()
	r := mux.NewRouter()
	r.HandleFunc("/track", auth(getTracks)).Methods("GET")
	r.HandleFunc("/track/{id}", auth(getTrack)).Methods("GET")
	r.HandleFunc("/track", auth(createTrack)).Methods("POST")
	r.HandleFunc("/signup", signup).Methods("POST")
	r.HandleFunc("/login", login).Methods("POST")
	return r
}

type errorDto struct {
	Message string `json:"message"`
	Error   string `json:"error"`
}
