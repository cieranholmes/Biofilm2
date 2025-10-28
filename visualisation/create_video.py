#!/usr/bin/env python3
"""
Script to create a video visualization of the biofilm simulation.
Produces a video with 100 ticks per second.
"""

import subprocess
import sys
import os

def main():
    print("Creating biofilm simulation video...")
    print("=" * 50)
    
    # Run the video visualization script with current Python
    print("Running video visualization...")
    try:
        result = subprocess.run([sys.executable, 'video_visualization.py'], 
                              capture_output=True, text=True, check=True)
        print(result.stdout)
        if result.stderr:
            print("Warnings/Errors:", result.stderr)
    except subprocess.CalledProcessError as e:
        print(f"Error running video visualization: {e}")
        print("Output:", e.stdout)
        print("Error:", e.stderr)
        return 1
    
    print("=" * 50)
    print("Video creation complete!")
    print("Check the output/ directory for the generated video file.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
