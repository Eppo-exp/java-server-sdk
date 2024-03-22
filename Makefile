# Make settings - @see https://tech.davis-hansson.com/p/make/
SHELL := bash
.ONESHELL:
.SHELLFLAGS := -eu -o pipefail -c
.DELETE_ON_ERROR:
MAKEFLAGS += --warn-undefined-variables
MAKEFLAGS += --no-builtin-rules

# Log levels
DEBUG := $(shell printf "\e[2D\e[35m")
INFO  := $(shell printf "\e[2D\e[36m🔵 ")
OK    := $(shell printf "\e[2D\e[32m🟢 ")
WARN  := $(shell printf "\e[2D\e[33m🟡 ")
ERROR := $(shell printf "\e[2D\e[31m🔴 ")
END   := $(shell printf "\e[0m")


.PHONY: default
default: help

## help - Print help message.
.PHONY: help
help: Makefile
	@echo "usage: make <target>"
	@sed -n 's/^##//p' $<

.PHONY: build
build: test-data
	mvn --batch-mode --update-snapshots package

## test-data
testDataDir := src/test/resources
banditsDataDir := ${testDataDir}/bandits
tempDir := ${testDataDir}/temp
tempBanditsDir := ${tempDir}/bandits
gitDataDir := ${tempDir}/sdk-test-data
branchName := main
githubRepoLink := https://github.com/Eppo-exp/sdk-test-data.git
.PHONY: test-data
test-data:
	find ${testDataDir} -mindepth 1 ! -regex '^${banditsDataDir}.*' -delete
	mkdir -p ${tempDir}
	git clone -b ${branchName} --depth 1 --single-branch ${githubRepoLink} ${gitDataDir}
	cp ${gitDataDir}/rac-experiments-v3.json ${testDataDir}
	cp -r ${gitDataDir}/assignment-v2 ${testDataDir}
	rm -rf ${tempDir}

.PHONY: test
test: test-data build
	mvn test
