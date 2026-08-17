#!/usr/bin/env -S uv run --script

# /// script
# requires-python = ">=3.14"
# dependencies = [
#     "iterfzf>=1.9.0.67.0",
#     "ollama>=0.6.2",
#     "python-dotenv>=1.2.3",
#     "rich>=15.0.0",
# ]
# ///

import os
from pathlib import Path
import shutil
import sys

from dotenv import load_dotenv
from iterfzf import iterfzf
from ollama import Client
from rich.prompt import Prompt, Confirm
from rich.console import Console
import rich.status


VIDEO_EXTENSIONS = {".mkv", ".avi", ".mp4", ".mov"}

def directories_containing_videos(root_dir):
    root = Path(root_dir)

    for path in root.rglob("*"):
        if path.is_file() and path.suffix.lower() in VIDEO_EXTENSIONS:
            yield str(path)


def get_videos_in_directory(dir):
	root = Path(dir)

	return sorted({
		path
        for path in root.iterdir()
        if path.is_file() and path.suffix.lower() in VIDEO_EXTENSIONS
	})


def call_ollama(message: str, ollama_api_key: str) -> str:
	client = Client(
    	host='https://ollama.com',
    	headers={'Authorization': 'Bearer ' + ollama_api_key}
	)

	messages = [
  		{
    		'role': 'user',
    		'content': message,
		},
	]

	ollama_response = []

	for part in client.chat('nemotron-3-nano:30b-cloud', messages=messages, stream=True):
        # print(part.message.content, end='', flush=True)
  		ollama_response.append(part.message.content)

	return "".join(ollama_response).strip()


def get_file_extension(filename: str) -> str:
	tmp = Path(filename)
	return tmp.suffix


def import_movie(original_content_path: str, title: str, dest: str) -> None:
	Path(f"{dest}/Movies/{title}").mkdir(parents=True, exist_ok=True)

	shutil.move(original_content_path, f"{dest}/Movies/{title}/{title}{get_file_extension(original_content_path)}")

	shutil.rmtree(os.path.dirname(original_content_path), ignore_errors=True)


def get_season_number(text: str) -> str:
	index_s = text.find('s')
	index_e = text.find('e')
	return text[index_s+1:index_e]


def get_episode_number(text: str) -> str:
	index_e = text.find('e')
	return text[index_e +1:]


def import_tvshow(original_content_path: str, title: str, dest: str) -> None:
	split_title = title.split('-')

	season_number = get_season_number(split_title[1])
	episode_number = get_episode_number(split_title[1])

	series_title = split_title[0].strip()

	Path(f"{dest}/TV Shows/{series_title}/Season {season_number}").mkdir(parents=True, exist_ok=True)

	shutil.move(original_content_path, f"{dest}/TV Shows/{series_title}/Season {season_number}/{title}{get_file_extension(original_content_path)}")

	shutil.rmtree(os.path.dirname(original_content_path), ignore_errors=True)


def main() -> None:
    load_dotenv()
    console = Console()

    SOURCE=os.getenv("SOURCE", "/Volumes/LightSpeed/containers/sabnzbd/config/Downloads/complete")
    DESTINATION=os.getenv("DESTINATION", "/Volumes/Content_Vault/Plex")
    OLLAMA_API_KEY=os.getenv("OLLAMA_API_KEY", "")

    if OLLAMA_API_KEY == "":
        print("your ollama api key is missing")
        return

    video_dirs = directories_containing_videos(SOURCE)

    try: 
        selected = iterfzf(video_dirs, multi=True)
    except AttributeError:
        print("no directories with videos were found")
        return
    except KeyboardInterrupt:
        print("User Cancelled")
        return

    for select in selected:
        video_base_dir = Path(select).parent.name
        choices=["Movie", "TV Show", "Skip", "Quit"]
        content_type_selection = iterfzf(choices, multi=False, prompt=f"Is {video_base_dir} a movie or tv show? > ")

        match content_type_selection:
            case "Movie":
                message = "what is the movie title and year in this directory name: {}\noutput in the following format: MOVIE TITLE (0000)".format(video_base_dir)
                with console.status("calling ollama...", spinner="dots"):
                    ollama_result = call_ollama(message, OLLAMA_API_KEY)
                title = Prompt.ask("Title: ", default=ollama_result)
                confirmation = iterfzf(["Yes", "No"], multi=False, prompt=f"is this correct?\n{title} >")
                if confirmation == "Yes":
                    import_movie(select, title, DESTINATION)
                else:
                    continue
            case "TV Show":
                message = "what is the series title, season number, and episode number in this directory name: {}\noutput in the following format: SERIES TITLE - s00e00".format(video_base_dir)
                with console.status("calling ollama...", spinner="dots"):
                    ollama_result = call_ollama(message, OLLAMA_API_KEY)
                title = Prompt.ask("Title: ", default=ollama_result)
                confirmation = iterfzf(["Yes", "No"], multi=False, prompt=f"is this correct?\n{title} >")
                if confirmation == "Yes":
                    import_tvshow(select, title, DESTINATION)
                else:
                    continue
            case "Skip":
                continue
            case _:
                return


if __name__ == "__main__":
    main()
