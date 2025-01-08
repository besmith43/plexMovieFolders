# TODO


### Must Haves

- make it cross platform with the file separator instead of hardcoded for unix ("/")

- imploy OneOf library to make it more *rusty*


### Questions?

- do I want a shared interface for the search, tvShow, and movie? (basically runnable)

- what features do I want in the toml config?

- do I want a sqlite db to act as a log sink?

- for the corona micro framework, do I need to make the tvShow and Movie classes registered services in order to have the dependency injection for a logger class?

- do I even want a DI logger instead of having the source of the log entry defined in the message structure?

- how do I unit test such a manual input heavy app?

- do I want to make an autocomplete class that takes in an enum and returns a specific autocomplete config?



### **Big Question**

- do I want to do a full redesign to use ssh and run this program from my laptop?

- do I want to build a Web API to handle this and a cli tui app to interface with it?

- or should it be a blazor app?


