# CMAKE generated file: DO NOT EDIT!
# Generated by "Unix Makefiles" Generator, CMake Version 2.8

#=============================================================================
# Special targets provided by cmake.

# Disable implicit rules so canonical targets will work.
.SUFFIXES:

# Remove some rules from gmake that .SUFFIXES does not remove.
SUFFIXES =

.SUFFIXES: .hpux_make_needs_suffix_list

# Suppress display of executed commands.
$(VERBOSE).SILENT:

# A target that is always out of date.
cmake_force:
.PHONY : cmake_force

#=============================================================================
# Set environment variables for the build.

# The shell in which to execute make rules.
SHELL = /bin/sh

# The CMake executable.
CMAKE_COMMAND = /usr/bin/cmake

# The command to remove a file.
RM = /usr/bin/cmake -E remove -f

# The top-level source directory on which CMake was run.
CMAKE_SOURCE_DIR = /media/OS/ANU/PhD/2014/codes/RegionGenerator/SensorNetworkTestCaseGenerator/gmmreg-master/gmmreg-master/C++

# The top-level build directory on which CMake was run.
CMAKE_BINARY_DIR = /media/OS/ANU/PhD/2014/codes/RegionGenerator/SensorNetworkTestCaseGenerator/gmmreg-master/gmmreg-master/C++

# Include any dependencies generated for this target.
include CMakeFiles/gmmreg_aux.dir/depend.make

# Include the progress variables for this target.
include CMakeFiles/gmmreg_aux.dir/progress.make

# Include the compile flags for this target's objects.
include CMakeFiles/gmmreg_aux.dir/flags.make

CMakeFiles/gmmreg_aux.dir/gmmreg_aux.o: CMakeFiles/gmmreg_aux.dir/flags.make
CMakeFiles/gmmreg_aux.dir/gmmreg_aux.o: gmmreg_aux.cpp
	$(CMAKE_COMMAND) -E cmake_progress_report /media/OS/ANU/PhD/2014/codes/RegionGenerator/SensorNetworkTestCaseGenerator/gmmreg-master/gmmreg-master/C++/CMakeFiles $(CMAKE_PROGRESS_1)
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Building CXX object CMakeFiles/gmmreg_aux.dir/gmmreg_aux.o"
	/usr/bin/c++   $(CXX_DEFINES) $(CXX_FLAGS) -o CMakeFiles/gmmreg_aux.dir/gmmreg_aux.o -c /media/OS/ANU/PhD/2014/codes/RegionGenerator/SensorNetworkTestCaseGenerator/gmmreg-master/gmmreg-master/C++/gmmreg_aux.cpp

CMakeFiles/gmmreg_aux.dir/gmmreg_aux.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Preprocessing CXX source to CMakeFiles/gmmreg_aux.dir/gmmreg_aux.i"
	/usr/bin/c++  $(CXX_DEFINES) $(CXX_FLAGS) -E /media/OS/ANU/PhD/2014/codes/RegionGenerator/SensorNetworkTestCaseGenerator/gmmreg-master/gmmreg-master/C++/gmmreg_aux.cpp > CMakeFiles/gmmreg_aux.dir/gmmreg_aux.i

CMakeFiles/gmmreg_aux.dir/gmmreg_aux.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Compiling CXX source to assembly CMakeFiles/gmmreg_aux.dir/gmmreg_aux.s"
	/usr/bin/c++  $(CXX_DEFINES) $(CXX_FLAGS) -S /media/OS/ANU/PhD/2014/codes/RegionGenerator/SensorNetworkTestCaseGenerator/gmmreg-master/gmmreg-master/C++/gmmreg_aux.cpp -o CMakeFiles/gmmreg_aux.dir/gmmreg_aux.s

CMakeFiles/gmmreg_aux.dir/gmmreg_aux.o.requires:
.PHONY : CMakeFiles/gmmreg_aux.dir/gmmreg_aux.o.requires

CMakeFiles/gmmreg_aux.dir/gmmreg_aux.o.provides: CMakeFiles/gmmreg_aux.dir/gmmreg_aux.o.requires
	$(MAKE) -f CMakeFiles/gmmreg_aux.dir/build.make CMakeFiles/gmmreg_aux.dir/gmmreg_aux.o.provides.build
.PHONY : CMakeFiles/gmmreg_aux.dir/gmmreg_aux.o.provides

CMakeFiles/gmmreg_aux.dir/gmmreg_aux.o.provides.build: CMakeFiles/gmmreg_aux.dir/gmmreg_aux.o

# Object files for target gmmreg_aux
gmmreg_aux_OBJECTS = \
"CMakeFiles/gmmreg_aux.dir/gmmreg_aux.o"

# External object files for target gmmreg_aux
gmmreg_aux_EXTERNAL_OBJECTS =

gmmreg_aux: CMakeFiles/gmmreg_aux.dir/gmmreg_aux.o
gmmreg_aux: CMakeFiles/gmmreg_aux.dir/build.make
gmmreg_aux: CMakeFiles/gmmreg_aux.dir/link.txt
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --red --bold "Linking CXX executable gmmreg_aux"
	$(CMAKE_COMMAND) -E cmake_link_script CMakeFiles/gmmreg_aux.dir/link.txt --verbose=$(VERBOSE)

# Rule to build all files generated by this target.
CMakeFiles/gmmreg_aux.dir/build: gmmreg_aux
.PHONY : CMakeFiles/gmmreg_aux.dir/build

CMakeFiles/gmmreg_aux.dir/requires: CMakeFiles/gmmreg_aux.dir/gmmreg_aux.o.requires
.PHONY : CMakeFiles/gmmreg_aux.dir/requires

CMakeFiles/gmmreg_aux.dir/clean:
	$(CMAKE_COMMAND) -P CMakeFiles/gmmreg_aux.dir/cmake_clean.cmake
.PHONY : CMakeFiles/gmmreg_aux.dir/clean

CMakeFiles/gmmreg_aux.dir/depend:
	cd /media/OS/ANU/PhD/2014/codes/RegionGenerator/SensorNetworkTestCaseGenerator/gmmreg-master/gmmreg-master/C++ && $(CMAKE_COMMAND) -E cmake_depends "Unix Makefiles" /media/OS/ANU/PhD/2014/codes/RegionGenerator/SensorNetworkTestCaseGenerator/gmmreg-master/gmmreg-master/C++ /media/OS/ANU/PhD/2014/codes/RegionGenerator/SensorNetworkTestCaseGenerator/gmmreg-master/gmmreg-master/C++ /media/OS/ANU/PhD/2014/codes/RegionGenerator/SensorNetworkTestCaseGenerator/gmmreg-master/gmmreg-master/C++ /media/OS/ANU/PhD/2014/codes/RegionGenerator/SensorNetworkTestCaseGenerator/gmmreg-master/gmmreg-master/C++ /media/OS/ANU/PhD/2014/codes/RegionGenerator/SensorNetworkTestCaseGenerator/gmmreg-master/gmmreg-master/C++/CMakeFiles/gmmreg_aux.dir/DependInfo.cmake --color=$(COLOR)
.PHONY : CMakeFiles/gmmreg_aux.dir/depend

