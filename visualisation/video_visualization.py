import matplotlib
matplotlib.use('Agg')  # Use non-interactive backend to avoid Qt font warnings
import matplotlib.pyplot as plt
import matplotlib.animation as animation
import pandas as pd
import numpy as np
from matplotlib.patches import FancyBboxPatch, Circle
import os

csv_file = 'input/simulation_output_part_001.csv'

if not os.path.exists(csv_file):
    print(f"CSV file '{csv_file}' not found!")
    exit(1)

print(f"Reading simulation data from: {csv_file}")

try:
    # Read the CSV file, skipping separator lines
    df = pd.read_csv(csv_file, low_memory=False)
    # Remove separator rows (they appear as NaN or contain '#' characters)
    df = df.dropna()
    df = df[~df.astype(str).apply(lambda x: x.str.contains('#')).any(axis=1)]
    
    # Convert tick_num to integer for proper sorting
    df['tick_num'] = pd.to_numeric(df['tick_num'], errors='coerce')
    df = df.dropna(subset=['tick_num'])
    df['tick_num'] = df['tick_num'].astype(int)
    
    # Convert numeric columns to proper types
    numeric_columns = ['pos_X', 'pos_Y', 'diameter', 'length', 'orientation_X', 'orientation_Y']
    for col in numeric_columns:
        if col in df.columns:
            df[col] = pd.to_numeric(df[col], errors='coerce')
    
    # Remove any rows with NaN values in numeric columns
    df = df.dropna(subset=numeric_columns)
    
except Exception as e:
    print(f"Error reading {csv_file}: {e}")
    exit(1)

# Get unique time steps
time_steps = sorted(df['tick_num'].unique())
print(f"Found {len(time_steps)} time steps")
print(f"Time step range: {time_steps[0]} to {time_steps[-1]}")

if time_steps:
    last_tick = time_steps[-1]
    last_tick_data = df[df['tick_num'] == last_tick]
    
    if not last_tick_data.empty:
        # Get min/max positions from all particles in the last tick
        min_x = last_tick_data['pos_X'].min()
        max_x = last_tick_data['pos_X'].max()
        min_y = last_tick_data['pos_Y'].min()
        max_y = last_tick_data['pos_Y'].max()
        
        # Add padding
        max_diameter = last_tick_data['diameter'].max() if 'diameter' in last_tick_data.columns else 1.0
        max_length = last_tick_data['length'].max() if 'length' in last_tick_data.columns else 1.0
        padding = max(max_diameter, max_length) * 2  # 2x largest particle dimension as padding
        
        plot_min_x = min_x - padding
        plot_max_x = max_x + padding
        plot_min_y = min_y - padding
        plot_max_y = max_y + padding
        
        print(f"Dynamic plot bounds based on last tick ({last_tick}): X=[{plot_min_x:.1f}, {plot_max_x:.1f}], Y=[{plot_min_y:.1f}, {plot_max_y:.1f}]")
    else:
        # Fallback if no data in last tick
        plot_min_x, plot_max_x = 0, 100
        plot_min_y, plot_max_y = 0, 100
        print("Warning: No data in last tick, using default bounds")
else:
    # Fallback if no ticks found
    plot_min_x, plot_max_x = 0, 100
    plot_min_y, plot_max_y = 0, 100
    print("Warning: No time steps found, using default bounds")

os.makedirs('output', exist_ok=True)

# Create figure and axis for animation
fig, ax = plt.subplots(figsize=(10, 10))
ax.set_aspect('equal')
ax.set_xlim(plot_min_x, plot_max_x)
ax.set_ylim(plot_min_y, plot_max_y)
ax.set_title('Biofilm Simulation Animation')
ax.set_xlabel('X Position (μm)')
ax.set_ylabel('Y Position (μm)')

# Initialize empty lists to store patches
eps_patches = []
cell_patches = []

def animate(frame):
    """Animation function called for each frame"""
    # Clear previous patches
    for patch in eps_patches + cell_patches:
        patch.remove()
    eps_patches.clear()
    cell_patches.clear()
    
    # Get current tick
    tick = time_steps[frame]
    
    # Filter data for current time step
    tick_data = df[df['tick_num'] == tick]
    
    cells = tick_data[tick_data['agent_type'] == 'cell']
    eps = tick_data[tick_data['agent_type'] == 'eps']

    # Draw EPS particles
    if not eps.empty:
        for _, eps_particle in eps.iterrows():
            radius = float(eps_particle['diameter']) / 2  # Convert diameter to radius
            eps_circle = Circle((float(eps_particle['pos_X']), float(eps_particle['pos_Y'])), 
                               radius, facecolor='red', edgecolor='darkred', 
                               alpha=0.7, linewidth=0.5)
            ax.add_patch(eps_circle)
            eps_patches.append(eps_circle)

    # Draw bacterial cells
    if not cells.empty:
        for _, cell in cells.iterrows():
            w = float(cell['diameter']); l = float(cell['length'])
            angle = np.arctan2(float(cell['orientation_Y']), float(cell['orientation_X']))
            angle_deg = np.degrees(angle)
            rect_len = l - w; rect_w = w; r = w / 2

            rect = FancyBboxPatch((-rect_len/2, -rect_w/2), rect_len, rect_w,
                                  boxstyle="round,pad=0", facecolor='cyan',
                                  edgecolor='none', alpha=0.8)
            left_cap = Circle((-rect_len/2, 0), r, facecolor='cyan', edgecolor='none', alpha=0.8)
            right_cap = Circle((rect_len/2, 0), r, facecolor='cyan', edgecolor='none', alpha=0.8)

            from matplotlib.transforms import Affine2D
            t = Affine2D().rotate_deg(angle_deg).translate(float(cell['pos_X']), float(cell['pos_Y']))
            rect.set_transform(t + ax.transData)
            left_cap.set_transform(t + ax.transData)
            right_cap.set_transform(t + ax.transData)

            ax.add_patch(rect); ax.add_patch(left_cap); ax.add_patch(right_cap)
            cell_patches.extend([rect, left_cap, right_cap])
    
    # Update title with current tick
    ax.set_title(f'Biofilm Simulation - Tick {tick}')
    
    return eps_patches + cell_patches

# Create animation
print(f"Creating animation with {len(time_steps)} frames...")
print("FPS: 60 ticks per second")
print("Duration: {:.2f} seconds".format(len(time_steps) / 60.0))

# Create the animation
anim = animation.FuncAnimation(fig, animate, frames=len(time_steps), 
                              interval=17, blit=False, repeat=True)

# Save as MP4 video
print("Saving video...")
try:
    # Use ffmpeg writer for MP4 output
    Writer = animation.writers['ffmpeg']
    writer = Writer(fps=60, metadata=dict(artist='Biofilm Simulation'), bitrate=1800)
    # Save video
    video_filename = 'output/biofilm_simulation_video.mp4'
    anim.save(video_filename, writer=writer)
    print(f"Video saved to: {video_filename}")
except Exception as e:
    print(f"Error saving video: {e}")
    print("Trying alternative method...")
    try:
        # Alternative: save as GIF
        gif_filename = 'output/biofilm_simulation_animation.gif'
        anim.save(gif_filename, writer='pillow', fps=60)
        print(f"Animation saved as GIF: {gif_filename}")
    except Exception as e2:
        print(f"Error saving GIF: {e2}")
        print("Showing animation in window instead...")
        plt.show()

print("Animation complete!")
