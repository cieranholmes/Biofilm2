# Biofilm Simulation

A comprehensive Java simulation of bacterial biofilm formation with Python visualization tools. This simulation models the growth, division, and interaction of bacterial cells in a nutrient-limited environment, including EPS (Extracellular Polymeric Substances) production and consumption. 

The simulation is based off of the [2023 Soft Matter article "Interplay of cell motility and self-secreted extracellular polymeric substance induced depletion effects on spatial patterning in a growing microbial colony" by Bera et al](https://pubs.rsc.org/en/content/articlelanding/2023/sm/d3sm01144e).

## Structure
- `simulation/` - Java simulation engine with multi-threaded physics
- `visualisation/` - Python visualization and analysis tools

## Requirements
- **Java 17+** (uses modern Java features including parallel streams)
- **Python 3.8+** with matplotlib, pandas, numpy, tkinter
- **Gradle** (included via gradlew wrapper)

## Key Features

### Simulation Engine
- **Multi-threaded physics** with parallel force calculations
- **Spatial hashing** for efficient neighbor detection
- **Nutrient field dynamics** with diffusion and consumption
- **Complex particle geometry** (spherocylinders with hemispherical caps)
- **Automatic file splitting** for large simulations (100,000 lines per file)
- **Research-based parameters** from Table S1 of the reference paper

### Particle Types
- **Bacterial cells**: Spherocylinders that grow, divide, and produce EPS
- **EPS particles**: Spherical particles representing extracellular polymeric substances
- **Nutrient field**: 2D diffusion field with consumption by bacteria

### Physics
- **Repulsive forces** between overlapping particles
- **Motility forces** for bacterial movement
- **Random forces** for thermal fluctuations
- **Overdamped dynamics** appropriate for biofilm systems

## Configuration

The simulation uses an INI-style configuration file (`config.ini`) with organized sections:

### Example Config Structure
```ini
# SIMULATION DOMAIN
[DOMAIN]
width=800                             # Simulation domain width (μm)
height=800                            # Simulation domain height (μm)

# BACTERIAL CELL PARAMETERS
[BACTERIA]
initial_count=1                       # Number of initial bacterial cells
length=5.0                            # Maximum length (lmax) = 5.0 μm
diameter=1.0                          # Diameter of cell (dc) = 1.0 μm
eps_diameter=0.5                      # Diameter of EPS particle (deps) = 0.5 μm
growth_rate=3.5                       # Linear growth rate (φ) = 3.5 μm/h
division_length=5.0                   # Cell division at maximum length = 5.0 μm
division_rate=1.0                     # Cell division rate (kdiv) = 1/h
eps_production_rate=1.0               # EPS production rate (keps) = 1.0/h

# FORCE PARAMETERS
[FORCES]
motility_force=0.0                    # Motility force (fmot)
em_eps_eps=400.0                      # Elastic modulus for EPS-EPS interactions 
em_eps_cell=700.0                     # Elastic modulus for EPS-Cell interactions 
em_cell_cell=400.0                    # Elastic modulus for Cell-Cell interactions

# FRICTION PARAMETERS
friction_coefficient_cell=200.0        # Friction coefficient (cell) = 200 Pa·h
friction_coefficient_eps=200.0         # Friction coefficient (EPS) = 200 Pa·h

# NUTRIENT PARAMETERS
nutrient_concentration=3.0             # Nutrient concentration (C0) = 3.0 fg·μm⁻³
nutrient_consumption_rate=1.0          # Nutrient consumption rate = 1.0/h
diffusion_rate=300.0                   # Diffusion rate of nutrient (D) = 300 μm²/h

# DENSITY THRESHOLDS
cell_density_threshold=5.0             # Threshold area-density of cell = 5.0 μm²
eps_density_threshold=0.3              # Threshold area-density of EPS = 0.3 μm²
local_sensing_radius=3.0               # Radius for local density calculation (μm)

# NUTRIENT GRID PARAMETERS
grid_width=800                         # Initial grid width (cells)
grid_height=800                        # Initial grid height (cells)
grid_cell_size=8.0                     # Size of each grid cell (μm)

# SIMULATION PARAMETERS
delta_time=0.01                        # Time step for physics integration
num_ticks=10000                        # Number of simulation time steps
```

## Quick Start

### Using Makefile (Recommended)
```bash
# Run complete simulation
make simulation

# Run visualization
make visualisation

# Create video from simulation data
make video

# Build simulation only
make build

# Clean build artifacts
make clean
```

### Manual Commands

#### Run Simulation
```bash
cd simulation

# Use default config
./gradlew run

# Use custom config file
./gradlew run --args="--config /path/to/your/config.ini"

# Use config from resources directory
./gradlew run --args="--config=src/main/resources/config.ini"
```
Generates `simulation_output_part_*.csv` files in `visualisation/input/`.

#### Visualize Results
```bash
cd visualisation
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt

# Launch main visualization app
python visualisation_app.py

# Or use the launcher
python launch_viewer.py
```

#### Visualization Options

1. ** Main Visualization App** (`visualisation_app.py`)
   - Full-featured interface with navigation controls
   - Jump to specific ticks, real-time statistics
   - Professional UI with Previous/Next buttons
   - Save images of current state

2. ** Video Creation** (`create_video.py`)
   - Creates MP4 videos from simulation data
   - Configurable frame rate and quality
   - Automatic processing of multiple part files


**Navigation**: Use arrow keys, mouse clicks, or type tick numbers to browse through simulation time steps.

## Command Line Options

### Simulation
- `--config <path>` - Specify configuration file path
  - Default: `src/main/resources/config.ini`

## Configuration Parameters

### Domain Settings
- **width, height**: Simulation domain dimensions in micrometers
- **grid_width, grid_height**: Nutrient field grid resolution
- **grid_cell_size**: Size of each nutrient grid cell in micrometers

### Bacterial Properties
- **initial_count**: Number of initial bacterial cells
- **length**: Maximum bacterial length (lmax) in micrometers
- **diameter**: Bacterial cell diameter (dc) in micrometers
- **growth_rate**: Linear growth rate (φ) in micrometers per hour
- **division_length**: Length threshold for cell division
- **division_rate**: Cell division rate (kdiv) per hour
- **eps_production_rate**: EPS production rate (keps) per hour

### EPS Properties
- **eps_diameter**: Diameter of EPS particles (deps) in micrometers

### Force Parameters
- **motility_force**: Motility force magnitude (fmot)
- **em_eps_eps**: Elastic modulus for EPS-EPS interactions
- **em_eps_cell**: Elastic modulus for EPS-Cell interactions
- **em_cell_cell**: Elastic modulus for Cell-Cell interactions

### Friction Parameters
- **friction_coefficient_cell**: Friction coefficient for bacterial cells (Pa·h)
- **friction_coefficient_eps**: Friction coefficient for EPS particles (Pa·h)

### Nutrient Field
- **nutrient_concentration**: Initial nutrient concentration (C0) in fg·μm⁻³
- **nutrient_consumption_rate**: Nutrient consumption rate per hour
- **diffusion_rate**: Nutrient diffusion rate (D) in μm²/h

### Density Thresholds
- **cell_density_threshold**: Threshold area-density for cell density calculations
- **eps_density_threshold**: Threshold area-density for EPS density calculations
- **local_sensing_radius**: Radius for local density calculations in micrometers

### Simulation Parameters
- **delta_time**: Time step for physics integration (smaller = more stable)
- **num_ticks**: Number of simulation time steps

## Output Format

The simulation generates CSV files with the following structure:
- **Filename**: `simulation_output_part_XXX.csv` (where XXX is zero-padded part number)
- **Automatic splitting**: Files are split at 100,000 lines to ensure data integrity
- **Columns**: `agent_id`, `tick_num`, `agent_type`, `pos_X`, `pos_Y`, `diameter`, `length`, `orientation_X`, `orientation_Y`
- **Particle types**: `cell` (bacterial cells), `eps` (EPS particles)
- **Tick separators**: `########################################` marks end of each time step

## Performance Features

### Multi-threading
- **Parallel force calculations** using Java parallel streams
- **Thread-local buffers** for collision detection
- **Spatial hashing** for O(1) neighbor lookups
- **Ping-pong buffering** for nutrient field updates

### Memory Management
- **Automatic file splitting** prevents memory issues with large simulations
- **Efficient data structures** for particle management
- **Streaming processing** for large datasets

## Research Integration

This simulation is based on research paper parameters from Table S1, implementing:
- **Spherocylinder geometry** for bacterial cells with hemispherical end caps
- **Monod kinetics** for nutrient-limited growth
- **Overdamped dynamics** appropriate for biofilm systems
- **Elastic contact forces** with different moduli for different particle types
- **Nutrient diffusion** with consumption by bacterial cells

## Troubleshooting

### Common Issues
1. **Java version**: Ensure Java 17+ is installed
2. **Memory issues**: Reduce `num_ticks` or increase JVM heap size
3. **File permissions**: Ensure write access to `visualisation/input/` directory
4. **Python dependencies**: Install all requirements from `requirements.txt`

### Performance Tips
- Use smaller `delta_time` for stability, larger for speed
- Adjust `grid_cell_size` to balance resolution vs. performance
- Monitor memory usage for large simulations
