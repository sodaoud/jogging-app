package router

import "github.com/gorilla/mux"

// New creates a new httprouter and add the handlers to it
func New() *mux.Router {
	r := mux.NewRouter()
	r.HandleFunc("/track", validate(getTracks)).Methods("GET")
	r.HandleFunc("/track", validate(createTrack)).Methods("POST")
	r.HandleFunc("/user", createUser).Methods("POST")
	r.HandleFunc("/login", login).Methods("POST")
	return r
}
