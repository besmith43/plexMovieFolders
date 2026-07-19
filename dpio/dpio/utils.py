from pathlib import Path
from ollama import Client
import shutil
import os

VIDEO_EXTENSIONS = {".mkv", ".avi", ".mp4", ".mov"}

def directories_containing_videos(root_dir):
    root = Path(root_dir)

    return sorted({
        path.parent
        for path in root.rglob("*")
        if path.is_file() and path.suffix.lower() in VIDEO_EXTENSIONS
    })


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
  		print(part.message.content, end='', flush=True)
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


if __name__ == "__main__":
	original_content_path = 'test_root_dir/My.Adventures.with.Superman.S03E04.1080p.WEB.h264-EDITH/file1.mkv'
	title = 'My Adventures with Superman - s03e04'
	dest = 'test_dest_dir'

	import_tvshow(original_content_path, title, dest)
