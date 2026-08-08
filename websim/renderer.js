// 2D Canvas Renderer
class Renderer {
    constructor(canvas) {
        this.canvas = canvas;
        this.ctx = canvas.getContext('2d');
        this.gridSize = 32;
        this.cellSize = 0;
        this.palette = this.createDefaultPalette();
    }

    createDefaultPalette() {
        // 27 distinct colors (Minecraft-inspired)
        return [
            '#4a4a4a', '#7f7f7f', '#999999', '#b3b3b3', '#cccccc', '#e6e6e6', '#ffffff',
            '#8b4513', '#a0522d', '#cd853f', '#deb887', '#d2691e', '#f4a460', '#ffa07a',
            '#2f4f2f', '#556b2f', '#6b8e23', '#808000', '#9acd32', '#adff2f', '#7cfc00',
            '#1e3a5f', '#2e5090', '#4169e1', '#6495ed', '#87ceeb', '#add8e6', '#b0e0e6'
        ];
    }

    setPalette(colors) {
        this.palette = colors;
    }

    setGridSize(size) {
        this.gridSize = size;
        this.cellSize = Math.floor(this.canvas.width / size);
    }

    clear() {
        this.ctx.fillStyle = '#1e1e1e';
        this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
    }

    // Render the pattern
    render(patternFn, size = 32) {
        this.setGridSize(size);
        this.clear();

        const startTime = performance.now();
        let blocksRendered = 0;
        let errors = 0;

        // Use world coordinates starting from 0,0
        for (let z = 0; z < size; z++) {
            for (let x = 0; x < size; x++) {
                try {
                    // Call pattern function with world coordinates
                    // In 2D view, we use Y=64 (typical ground level)
                    const paletteIndex = patternFn(x, 64, z);
                    
                    if (paletteIndex !== null && paletteIndex !== undefined && !isNaN(paletteIndex)) {
                        const index = Math.floor(paletteIndex);
                        if (index >= 0 && index < this.palette.length) {
                            const color = this.palette[index];
                            this.ctx.fillStyle = color;
                            
                            const px = x * this.cellSize;
                            const py = z * this.cellSize;
                            
                            this.ctx.fillRect(px, py, this.cellSize, this.cellSize);
                            blocksRendered++;
                        }
                    }
                } catch (e) {
                    errors++;
                    if (errors === 1) {
                        console.error('Render error at', x, z, ':', e);
                    }
                }
            }
        }

        // Draw grid lines if cells are large enough
        if (this.cellSize >= 8) {
            this.drawGrid();
        }

        const renderTime = performance.now() - startTime;
        
        if (errors > 0) {
            console.warn(`Rendering completed with ${errors} errors`);
        }
        
        return { blocksRendered, renderTime, errors };
    }

    drawGrid() {
        this.ctx.strokeStyle = 'rgba(255, 255, 255, 0.1)';
        this.ctx.lineWidth = 1;

        // Vertical lines
        for (let x = 0; x <= this.gridSize; x++) {
            const px = x * this.cellSize;
            this.ctx.beginPath();
            this.ctx.moveTo(px, 0);
            this.ctx.lineTo(px, this.canvas.height);
            this.ctx.stroke();
        }

        // Horizontal lines
        for (let z = 0; z <= this.gridSize; z++) {
            const pz = z * this.cellSize;
            this.ctx.beginPath();
            this.ctx.moveTo(0, pz);
            this.ctx.lineTo(this.canvas.width, pz);
            this.ctx.stroke();
        }
    }

    // Get grid coordinates from mouse position
    getGridCoords(mouseX, mouseY) {
        const rect = this.canvas.getBoundingClientRect();
        const x = mouseX - rect.left;
        const y = mouseY - rect.top;

        const gridX = Math.floor(x / this.cellSize);
        const gridZ = Math.floor(y / this.cellSize);

        if (gridX >= 0 && gridX < this.gridSize && gridZ >= 0 && gridZ < this.gridSize) {
            return { x: gridX, z: gridZ };
        }

        return null;
    }
}
