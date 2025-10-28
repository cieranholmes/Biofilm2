# Biofilm Simulation Makefile

.PHONY: all run simulation visualisation build clean test build-run rebuild help

# Variables
GRADLEW = ./gradlew
VENV_PATH = venv/bin/activate
VISUALISATION_SCRIPT = visualisation_app.py
VISUALISATION_VIDEO_SCRIPT = create_video.py

all: simulation

run: simulation

simulation:
	rm -rf visualisation/input/ && mkdir -p visualisation/input && cd simulation && $(GRADLEW) run

visualisation:
	cd visualisation && python $(VISUALISATION_SCRIPT)

video:
	cd visualisation && python $(VISUALISATION_VIDEO_SCRIPT)

build:
	cd simulation && $(GRADLEW) build

clean:
	rm -rf visualisation/input/* && rm -rf visualisation/output/* && cd simulation && $(GRADLEW) clean

test:
	cd simulation && $(GRADLEW) test
