# Biofilm Simulation - User Manual

## Table of Contents
1. [Overview](#overview)
2. [System Requirements](#system-requirements)
3. [Installation](#installation)
4. [Quick Start Guide](#quick-start-guide)
5. [Configuration](#configuration)
6. [Running Simulations](#running-simulations)
7. [Visualization Tools](#visualization-tools)
8. [Video Creation](#video-creation)
9. [Advanced Usage](#advanced-usage)
10. [Performance Tips](#performance-tips)

---

## Overview

The Biofilm Simulation is a computational model that simulates the growth and spatial patterning of bacterial biofilms based on individual-based modeling principles. The simulation models the complex interactions between bacteria, extracellular polymeric substances (EPS), and nutrient fields to understand how microscopic interactions scale up to determine macroscopic biofilm structure.

### Key Features
- **Individual-based modeling** of bacterial cells and EPS particles
- **Multi-threaded simulation engine** for high performance
- **Realistic physics** including repulsive forces, motility, and random motion
- **Nutrient field simulation** with diffusion and consumption
- **Multiple visualization tools** for analysis and presentation
- **Video generation** capabilities for dynamic visualization
- **Configurable parameters** based on research paper specifications

---

## System Requirements

### Minimum Requirements
- **Java**: Version 17 or higher
- **Python**: Version 3.8 or higher
- **RAM**: 4 GB minimum, 8 GB recommended
- **Storage**: 1 GB free space
- **OS**: Windows 10+, macOS 10.14+, or Linux

### Recommended Requirements
- **Java**: Version 17 or higher
- **Python**: Version 3.9 or higher
- **RAM**: 16 GB or more
- **CPU**: Multi-core processor (4+ cores)
- **Storage**: 5 GB free space

---

## Installation


### 1. Install Java Dependencies
The simulation uses Gradle for dependency management. No additional Java installation is required beyond Java 17+.

### 2. Install Python Dependencies
```bash
cd visualisation
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```

### 3. Verify Installation
```bash
# Test Java simulation
cd simulation
./gradlew build

# Test Python visualization
cd ../visualisation
python -c "import matplotlib, pandas, numpy; print('Python dependencies OK')"
```

---

## Quick Start Guide

### Step 1: Run a Simulation
```bash
# From project root
# Note ensure line endings match your operating system (Windows: CRLF, macOS/Linux: LF).
make simulation
# OR
cd simulation && ./gradlew run
```

### Step 2: Visualize Results
```bash
# From project root
make visualisation
# OR
cd visualisation && python launch_viewer.py
```

### Step 3: Create a Video (Optional)
```bash
# From project root
make video
# OR
cd visualisation && python create_video.py
```

---

## Configuration

The simulation behavior is controlled through the `config.ini` file located in `simulation/src/main/resources/config.ini`.

### Configuration Sections

#### Domain Settings
```ini
[DOMAIN]
width=800                    # Simulation domain width (μm)
height=800                   # Simulation domain height (μm)
```

#### Bacterial Parameters
```ini
[BACTERIA]
initial_count=1              # Number of initial bacterial cells
length=5.0                   # Maximum length (μm)
diameter=1.0                 # Cell diameter (μm)
eps_diameter=0.5             # EPS particle diameter (μm)
growth_rate=3.5              # Linear growth rate (μm/h)
division_length=5.0          # Cell division threshold (μm)
division_rate=1.0            # Cell division rate (/h)
eps_production_rate=1.0      # EPS production rate (/h)
```

#### Force Parameters
```ini
[FORCES]
motility_force=1100.0        # Motility force magnitude

# Elastic Modulus Parameters
em_eps_eps=400.0            # EPS-EPS interaction strength
em_eps_cell=700.0           # EPS-Cell interaction strength
em_cell_cell=400.0          # Cell-Cell interaction strength

# Friction Parameters
friction_coefficient_cell=200.0  # Cell friction coefficient (Pa·h)
friction_coefficient_eps=200.0   # EPS friction coefficient (Pa·h)
```

#### Nutrient Parameters
```ini
# Nutrient Field
nutrient_concentration=3.0      # Initial concentration (fg·μm⁻³)
nutrient_consumption_rate=1.0   # Consumption rate (/h)
diffusion_rate=300.0            # Diffusion rate (μm²/h)

# Grid Parameters
grid_width=800                  # Nutrient grid width (cells)
grid_height=800                 # Nutrient grid height (cells)
grid_cell_size=8.0              # Grid cell size (μm)
```

#### Simulation Parameters
```ini
delta_time=0.01                 # Time step (smaller = more stable)
num_ticks=10000                 # Total simulation steps
```

### Parameter Guidelines

- **Domain Size**: Larger domains allow for more complex patterns but require more computation
- **Time Step**: Smaller values (0.001-0.01) provide better stability but slower execution
- **Force Magnitudes**: Based on research paper specifications; modify carefully
- **Nutrient Parameters**: Affect biofilm growth patterns significantly

---

## Running Simulations

### Basic Simulation
```bash
cd simulation
./gradlew run
```

### Custom Configuration
```bash
# Use specific config file
./gradlew run --args="--config /path/to/custom/config.ini"

# Use config from resources
./gradlew run --args="--config=src/main/resources/config.ini"
```

### Simulation Output
The simulation generates multiple CSV files in the format:
- `simulation_output_part_001.csv`
- `simulation_output_part_002.csv`
- etc.

Each file contains up to 100,000 lines and represents a portion of the simulation timeline.

### Output Format
Each CSV file contains the following columns:
- `agent_id`: Unique particle identifier
- `tick_num`: Simulation time step
- `agent_type`: "cell" or "eps"
- `pos_X`, `pos_Y`: Position coordinates (μm)
- `diameter`: Particle diameter (μm)
- `length`: Particle length (μm) - 0 for EPS
- `orientation_X`, `orientation_Y`: Orientation vector

---

## Visualization Tools

The visualization system processes CSV output files generated by the Java simulation to create interactive and static visual representations of biofilm development. The system reads particle data from `simulation_output_part_*.csv` files, which contain columns for particle ID, tick number, type (cell/EPS), position coordinates, diameter, length, and orientation vectors. The visualization uses matplotlib to render bacterial cells as elongated spherocylinders and EPS particles as circles, with colors distinguishing between particle types.

### Main Visualization App
```bash
cd visualisation
python visualisation_app.py
```
The main visualization app displays the final state of the simulation as a visual and allows saving the visual as a PNG file. It provides a simple interface to view the last tick of the simulation data.

### Interactive Viewer
```bash
cd visualisation
python interactive_viz.py
```
The interactive viewer provides arrow key navigation through different time steps to observe biofilm growth, division, and EPS production over time.

### Video Creation
```bash
cd visualisation
python create_video.py
```
Creates animated MP4 videos by processing sequential time steps from the simulation data.

---

## Video Creation

### Creating MP4 Videos
```bash
cd visualisation
python create_video.py
```

### Video Configuration
The video creation script (`video_visualization.py`) can be customized:

- **Frame Rate**: Currently set to 15 FPS
- **Duration**: Based on number of time steps
- **Output Format**: MP4 (with GIF fallback)
- **File Source**: Uses first part file by default

### Video Output
- **Primary**: `output/biofilm_simulation_video.mp4`
- **Fallback**: `output/biofilm_simulation_animation.gif`
- **Size**: Varies based on simulation complexity and duration

### Customizing Video Creation
Edit `video_visualization.py` to modify:
- Frame rate (line 163: `fps=15`)
- Video quality (line 165: `bitrate=1800`)
- Time step range (remove line 48 for full simulation)

---

## Advanced Usage

### Using the Makefile
The project includes a Makefile for convenient operations:

```bash
# Run complete simulation
make simulation

# Launch visualization
make visualisation

# Create video
make video

# Build simulation
make build

# Run tests
make test

# Clean build files
make clean
```

### Custom Simulation Parameters
1. Edit `simulation/src/main/resources/config.ini`
2. Modify parameters as needed
3. Run simulation with: `make simulation`

```
### Performance Optimization
- **Time Steps**: Reduce `num_ticks` for quicker results
- **Time Step Size**: Larger `delta_time` values run faster but may be less stable

---

### Getting Help
1. Check the console output for error messages
2. Verify all dependencies are installed
3. Ensure configuration parameters are valid
4. Check available disk space and memory

---

### Key Files
- `simulation/src/main/resources/config.ini`: Main configuration
- `visualisation/launch_viewer.py`: Visualization launcher
- `visualisation/video_visualization.py`: Video creation script
- `Makefile`: Convenience commands

---

## Performance Tips

### Simulation Performance
1. **Optimize Domain Size**: Balance between detail and performance
2. **Adjust Time Steps**: Larger steps = faster but less stable
3. **Monitor Memory**: Large simulations require significant RAM

### Visualization Performance
1. **Use Appropriate Viewer**: Tkinter for detailed work, static for overview
2. **Limit Time Steps**: For video creation, consider using subsets
3. **Close Unused Applications**: Free up system resources
4. **Use SSD Storage**: Faster I/O for large data files

### System Optimization
1. **Ensure Adequate RAM**: 8GB+ recommended for large simulations
2. **Use Fast Storage**: SSD recommended for data files
3. **Close Background Applications**: Free up CPU and memory
4. **Monitor System Resources**: Use task manager to check usage

---

## Conclusion

The Biofilm Simulation provides a powerful platform for studying bacterial biofilm formation through individual-based modeling. With its multi-threaded simulation engine and comprehensive visualization tools, it enables researchers to explore the complex dynamics of biofilm growth and spatial patterning.

For additional support or questions, refer to the project documentation or contact the development team.

---

*Last updated: September 2024*
*Version: 1.0*
