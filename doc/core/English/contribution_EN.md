# How to Contribute

## Submitting a Bug

If you have found a non-security-related bug in AGL, please search the Issues first to check if the bug has already been
reported. If not, create a new Issue to describe the bug.

## Submitting a Security Issue

If you have discovered a security issue in AGL, please do not disclose it publicly. Instead, contact the owner via email
and provide a detailed description of the security issue.

## Resolving Existing Issues

By reviewing the list of Issues in the repository, you can find problems that need to be addressed. You can try to
resolve one of these issues.

## Setting up the Development Environment

Please refer to the [Installation Guide](install_EN.md)

## Code Style Guidelines

### Python

#### Code Style

The code style for Python should generally follow the PEP 8 standard.
If you are using PyCharm as your development environment, you can use
the [BlackConnect](https://black.readthedocs.io/en/stable/integrations/editors.html) plugin for code formatting.

#### Docstring

Use the Google style format for docstrings.
If you are using PyCharm, you can configure it in Preferences -> Tools -> Python Integrated Tools -> Docstrings.

### C++

#### Code Style

Use the Google Style for C++ code.
If you are using CLion as your development environment, you can configure and format the code by following the
instructions provided in this [link](https://www.jetbrains.com/help/clion/predefined-code-styles.html).

### JAVA

#### Code style

Use the Google Style for Java code.
If you are using IntelliJ as your development environment, you can configure and format the code by following these
steps:
(1) Download
the [intellij-java-google-style.xml](https://github.com/google/styleguide/blob/gh-pages/intellij-java-google-style.xml)
file.

(2) In IntelliJ, go to Settings -> Editor -> Code Style -> Import Scheme -> IntelliJ IDEA code style XML and configure
it by importing the downloaded XML file.

## Issues and Pull Requests

For graph sampling process and graph data format (GraphFeature), we generally try not to make frequent modifications to
ensure consistency across different versions. Therefore, it is recommended to discuss any issues or concerns through
issues first.

However, we welcome contributions related to functionality improvements, feature expansions, bug fixes, and other areas.
Feel free to submit pull requests for such modifications.

## Development Workflow

* Switch to your development branch
  ```
  git checkout -b your-branch
  ....
  git add xxx
  git commit -m "xxx"
  ```
* Develop your feature
  xxxx
* Add unit tests To run unit tests:
  ```bash
  # c++ unit tests
  bash ./script/cpp_ut.sh
  # python unit tests
  bash ./script/python_ut.sh
  # java unit tests
  mvn clean package # compile and run the unit tests
  ```
* Submit a Pull Request (PR)

  xxx
* Resolve conflicts
  ```
  git checkout your-branch
  git rebase master # make sure your local master is up to date
  ```
* Code review

  Your submitted code will need to pass a code review before it can be merged into the master branch. Please be patient
  as we assign relevant team members for code review. If there is no response from the assigned reviewers within 4
  working days, please mention them in your PR. Code review comments will be directly posted in the relevant PR or
  issue. If you find the suggestions reasonable, please update your code accordingly.

* Merge into master

  After the code review is approved, we will assign another reviewer to conduct a further review. At least two reviewers
  need to agree on the PR before it can be merged into the main codebase. During this process, there may be some
  suggestions for modifications. Please be patient and make the necessary changes. Once all requirements are met, the PR
  will be merged into the master branch.