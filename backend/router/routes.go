package router

import "github.com/gorilla/mux"

// New creates a new httprouter and add the handlers to it
func New() *mux.Router {
	r := mux.NewRouter()
	r.HandleFunc("/track", auth(getTracks)).Methods("GET")
	r.HandleFunc("/track/{id}", auth(getTrack)).Methods("GET")
	r.HandleFunc("/track/{id}", auth(updateTrack)).Methods("PUT")
	r.HandleFunc("/track/{id}", auth(deleteTrack)).Methods("DELETE")
	r.HandleFunc("/track", auth(createTrack)).Methods("POST")

	r.HandleFunc("/user", auth(manager(getUsers))).Methods("GET")
	r.HandleFunc("/user/{id}", auth(manager(updateUser))).Methods("PUT")
	r.HandleFunc("/user/{id}", auth(manager(deleteUser))).Methods("DELETE")
	r.HandleFunc("/user", auth(manager(createUser))).Methods("POST")

	r.HandleFunc("/signup", signup).Methods("POST")
	r.HandleFunc("/login", login).Methods("POST")
	return r
}

type errorDto struct {
	Message string `json:"message"`
	Error   string `json:"error"`
}
