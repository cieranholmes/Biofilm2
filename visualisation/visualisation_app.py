import tkinter as tk
from tkinter import ttk
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
from matplotlib.patches import FancyBboxPatch, Circle
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
import os

class TkinterBiofilmViewer:
    def __init__(self, csv_file):
        self.csv_file = csv_file
        self.time_steps = []
        self.df = None
        
        self.load_data()
        
        self.setup_plot_bounds()
        
        self.create_gui()
        
    def load_data(self):
        """Load and process the CSV data"""
        if not os.path.exists(self.csv_file):
            print(f"CSV file '{self.csv_file}' not found!")
            exit(1)

        print(f"Reading simulation data from: {self.csv_file}")

        try:
            # Read the CSV file, skipping separator lines
            self.df = pd.read_csv(self.csv_file, low_memory=False)
            # Remove separator rows (they appear as NaN or contain '#' characters)
            self.df = self.df.dropna()
            self.df = self.df[~self.df.astype(str).apply(lambda x: x.str.contains('#')).any(axis=1)]
            
            # Convert tick_num to integer for proper sorting
            self.df['tick_num'] = pd.to_numeric(self.df['tick_num'], errors='coerce')
            self.df = self.df.dropna(subset=['tick_num'])
            self.df['tick_num'] = self.df['tick_num'].astype(int)
            
        except Exception as e:
            print(f"Error reading {self.csv_file}: {e}")
            exit(1)

        # Get unique time steps
        self.time_steps = sorted(self.df['tick_num'].unique())
        print(f"Found {len(self.time_steps)} time steps: {self.time_steps}")
        
    def setup_plot_bounds(self):
        """Calculate plot bounds based on the last tick data"""
        if self.time_steps:
            last_tick = self.time_steps[-1]
            last_tick_data = self.df[self.df['tick_num'] == last_tick]
            
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
                
                self.plot_min_x = min_x - padding
                self.plot_max_x = max_x + padding
                self.plot_min_y = min_y - padding
                self.plot_max_y = max_y + padding
                
                print(f"Dynamic plot bounds: X=[{self.plot_min_x:.1f}, {self.plot_max_x:.1f}], Y=[{self.plot_min_y:.1f}, {self.plot_max_y:.1f}]")
            else:
                # Fallback if no data in last tick
                self.plot_min_x, self.plot_max_x = 0, 100
                self.plot_min_y, self.plot_max_y = 0, 100
                print("Warning: No data in last tick, using default bounds")
        else:
            # Fallback if no ticks found
            self.plot_min_x, self.plot_max_x = 0, 100
            self.plot_min_y, self.plot_max_y = 0, 100
            print("Warning: No time steps found, using default bounds")
            
    def create_gui(self):
        """Create the tkinter GUI"""
        self.root = tk.Tk()
        part_name = os.path.basename(self.csv_file).replace('.csv', '')
        self.root.title(f"Biofilm Simulation - Final State ({part_name})")
        self.root.geometry("1200x800")
        
        self.root.protocol("WM_DELETE_WINDOW", self.on_closing)
        
        # Create main frame
        main_frame = ttk.Frame(self.root)
        main_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)
        
        # Create matplotlib figure
        self.fig, self.ax = plt.subplots(figsize=(10, 6))
        self.fig.patch.set_facecolor('white')
        
        # Create canvas for matplotlib
        canvas_frame = ttk.Frame(main_frame)
        canvas_frame.pack(fill=tk.BOTH, expand=True)
        
        self.canvas = FigureCanvasTkAgg(self.fig, canvas_frame)
        self.canvas.get_tk_widget().pack(fill=tk.BOTH, expand=True)
        
        # Create info frame
        info_frame = ttk.Frame(main_frame)
        info_frame.pack(fill=tk.X, pady=(10, 0))
        
        # Show final tick info
        final_tick = self.time_steps[-1] if self.time_steps else 0
        self.tick_label = ttk.Label(info_frame, text=f"Final Tick: {final_tick}")
        self.tick_label.pack(side=tk.LEFT)
        
        # Status frame
        status_frame = ttk.Frame(main_frame)
        status_frame.pack(fill=tk.X, pady=(5, 0))
        
        self.status_label = ttk.Label(status_frame, text="Showing final simulation state")
        self.status_label.pack(side=tk.LEFT)
        
        # Plot the final state
        self.plot_final_state()
        
    def plot_final_state(self):
        """Plot the final state of the simulation"""
        self.ax.clear()
        
        if not self.time_steps:
            self.ax.text(0.5, 0.5, 'No data available', 
                        transform=self.ax.transAxes, ha='center', va='center')
            return
            
        final_tick = self.time_steps[-1]
        
        # Filter data for final time step
        tick_data = self.df[self.df['tick_num'] == final_tick]
        
        cells = tick_data[tick_data['agent_type'] == 'cell']
        eps = tick_data[tick_data['agent_type'] == 'eps']

        # Draw EPS particles
        if not eps.empty:
            for _, eps_particle in eps.iterrows():
                radius = eps_particle['diameter'] / 2  # Convert diameter to radius
                eps_circle = Circle((eps_particle['pos_X'], eps_particle['pos_Y']), 
                                   radius, facecolor='red', edgecolor='darkred', 
                                   alpha=0.7, linewidth=0.5)
                self.ax.add_patch(eps_circle)

        # Draw bacterial cells
        if not cells.empty:
            for _, cell in cells.iterrows():
                w = cell['diameter']
                l = cell['length']
                angle = np.arctan2(cell['orientation_Y'], cell['orientation_X'])
                angle_deg = np.degrees(angle)
                rect_len = l - w
                rect_w = w
                r = w / 2

                # Create cell body (rectangle)
                rect = FancyBboxPatch((-rect_len/2, -rect_w/2), rect_len, rect_w,
                                      boxstyle="round,pad=0", facecolor='cyan',
                                      edgecolor='none', alpha=0.8)
                
                # Create cell caps (circles)
                left_cap = Circle((-rect_len/2, 0), r, facecolor='cyan', edgecolor='none', alpha=0.8)
                right_cap = Circle((rect_len/2, 0), r, facecolor='cyan', edgecolor='none', alpha=0.8)

                # Apply transformation (rotation and translation)
                from matplotlib.transforms import Affine2D
                t = Affine2D().rotate_deg(angle_deg).translate(cell['pos_X'], cell['pos_Y'])
                rect.set_transform(t + self.ax.transData)
                left_cap.set_transform(t + self.ax.transData)
                right_cap.set_transform(t + self.ax.transData)

                self.ax.add_patch(rect)
                self.ax.add_patch(left_cap)
                self.ax.add_patch(right_cap)

        # Set plot properties
        self.ax.set_aspect('equal')
        self.ax.set_xlim(self.plot_min_x, self.plot_max_x)
        self.ax.set_ylim(self.plot_min_y, self.plot_max_y)
        
        # Set title with final tick info
        cell_count = len(cells)
        eps_count = len(eps)
        title = f'Biofilm Simulation - Final State (Tick {final_tick})'
        self.ax.set_title(title, fontsize=14, fontweight='bold')
        
        # Remove axis ticks for cleaner look
        self.ax.set_xticks([])
        self.ax.set_yticks([])
        
        # Add legend
        legend_elements = []
        if cell_count > 0:
            legend_elements.append(plt.Line2D([0], [0], marker='o', color='w', markerfacecolor='cyan', 
                                            markersize=10, label=f'Bacterial Cells ({cell_count})'))
        if eps_count > 0:
            legend_elements.append(plt.Line2D([0], [0], marker='o', color='w', markerfacecolor='red', 
                                            markersize=8, label=f'EPS Particles ({eps_count})'))
        
        if legend_elements:
            self.ax.legend(handles=legend_elements, loc='upper right')
        
        # Update status
        self.status_label.config(text=f"Final state: {cell_count} cells, {eps_count} EPS particles")
        
        # Refresh the plot
        self.canvas.draw()
        
        # Save the image
        self.save_image(final_tick, cell_count, eps_count)
        
    def save_image(self, final_tick, cell_count, eps_count):
        """Save the final state plot as an image file"""
        try:
            # Create output directory if it doesn't exist
            os.makedirs('output', exist_ok=True)
            
            # Generate filename with part info and tick number
            part_name = os.path.basename(self.csv_file).replace('.csv', '')
            filename = f'output/biofilm_final_state_{part_name}_tick{final_tick}.png'
            
            # Save the figure
            self.fig.savefig(filename, dpi=300, bbox_inches='tight', facecolor='white')
            print(f"Image saved as: {filename}")
            
        except Exception as e:
            print(f"Error saving image: {e}")
            print("Continuing with display only...")
    
    def on_closing(self):
        """Handle window close event"""
        print("Closing application...")
        try:
            # Close matplotlib figure to free memory
            plt.close(self.fig)
        except:
            pass
        try:
            # Destroy the tkinter window
            self.root.destroy()
        except:
            pass
        # Force exit the Python process
        import sys
        print("Application closed successfully.")
        sys.exit(0)
        
    def run(self):
        """Start the GUI"""
        part_name = os.path.basename(self.csv_file).replace('.csv', '')
        print("\n" + "="*60)
        print("BIOFILM SIMULATION - FINAL STATE VIEWER")
        print("="*60)
        print(f"Showing the final state from: {part_name}")
        print("Images will be saved to the output/ directory")
        print("Close window to exit")
        print("="*60)
        
        try:
            self.root.mainloop()
        except KeyboardInterrupt:
            print("\nApplication interrupted by user")
            self.on_closing()
        except Exception as e:
            print(f"Unexpected error: {e}")
            self.on_closing()

# Main execution
if __name__ == "__main__":
    # Create output directory
    os.makedirs('output', exist_ok=True)
    
    # Find the highest numbered part file
    input_dir = 'input'
    part_files = []
    
    if os.path.exists(input_dir):
        for file in os.listdir(input_dir):
            if file.startswith('simulation_output_part_') and file.endswith('.csv'):
                part_files.append(file)
    
    if not part_files:
        print("No part files found in input directory!")
        exit(1)
    
    # Sort by part number and get the highest
    part_files.sort(key=lambda x: int(x.split('_part_')[1].split('.')[0]))
    highest_part_file = part_files[-1]
    
    print(f"Found {len(part_files)} part files")
    print(f"Using highest numbered file: {highest_part_file}")
    
    csv_file = os.path.join(input_dir, highest_part_file)
    viewer = TkinterBiofilmViewer(csv_file)
    viewer.run()
