from django.http import HttpResponse, HttpResponseBadRequest
from django.template.loader import get_template
from django.shortcuts import render, redirect
from . import utils
import os

SOURCE=os.getenv("SOURCE")
DESTINATION=os.getenv("DESTINATION")
OLLAMA_API_KEY=os.getenv("OLLAMA_API_KEY")

print(SOURCE)
print(DESTINATION)

def index(request):
	potential_content_dirs = utils.directories_containing_videos(SOURCE)

	# return HttpResponse("Hello World")
	return render(request, 'index.html', {"source": SOURCE, "destination": DESTINATION, "dirs": potential_content_dirs})


def content(request):
	selected_content = request.POST.get("selected_content")

	if selected_content is None:
		return HttpResponseBadRequest("no selected content provided")

	videos = utils.get_videos_in_directory(selected_content)
	return render(request, 'select_video.html', {"videos": videos})


def guess(request):
	selected_content = request.POST.get("content_selection")
	selected_type = request.POST.get("type_selection")

	if selected_content is None:
		return HttpResponseBadRequest("no selected content provided")


	if selected_type is None:
		return HttpResponseBadRequest("no selected type provided")

	tmp = selected_content.split("/")

	split_length = len(tmp)

	base_dir_name = tmp[split_length-2]

	print(base_dir_name)

	message = None

	if selected_type == "movie":
		message = "what is the movie title and year in this directory name: {}\noutput in the following format: MOVIE TITLE (0000)".format(base_dir_name)
	elif selected_type == "tvshow":
		message = "what is the series title, season number, and episode number in this directory name: {}\noutput in the following format: SERIES TITLE - s00e00".format(base_dir_name)
	else:
		return HttpResponseBadRequest("selected type can only be movie or tvshow")

	ollama_result = utils.call_ollama(message, OLLAMA_API_KEY)
	print(ollama_result)

	return render(request, 'guess.html', {"guess": ollama_result.strip()})


def submit(request):
	selected_content = request.POST.get("content_selection")
	selected_type = request.POST.get("type_selection")
	title = request.POST.get("content_name")

	print(selected_content)
	print(selected_type)
	print(title)

	if selected_type == "movie":
		utils.import_movie(selected_content, title, DESTINATION)
	elif selected_type == "tvshow":
		utils.import_tvshow(selected_content, title, DESTINATION)
	else:
		return HttpResponseBadRequest("selected type can only be movie or tvshow")

	return HttpResponse("yay")