package org.xenei.robot.mapper.visualization;


    import java.awt.Color;
    import java.awt.Graphics;
    import java.util.ArrayList;
    import java.util.List;

    import javax.swing.JPanel;
    /**
     * 
     * @see https://www.smartycoder.com
     *
     */
    @SuppressWarnings("serial")
    public class JTSPanel extends JPanel {

        private List<DrawingCommand> drawPathCommand = new ArrayList<>();
        
        public JTSPanel(){
            setSize(900, 900);
        
        }
        
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
     
            g.setColor(Color.darkGray);
            
            g.fillRect(0, 0, 1000, 1000);
            
            g.setColor(Color.WHITE);
            
            g.fillRect(10, 10, 4, 4);
         
            g.drawLine(10, 10, 10, 1000);
            
            g.drawLine(10, 10, 1000, 10);
            
            for(int i = 10; i <= 1000; i += 50) {
                
                g.drawString(Integer.toString(i), i, 10);
                
                g.drawString(Integer.toString(i), 10, i);
            }
            
            g.setColor(Color.BLACK);
            
            for (DrawingCommand drawingCommand : drawPathCommand) {
                
                drawingCommand.doDrawing(g);
            }
            
        }

        public void addDrawCommand(DrawingCommand c) {
            this.drawPathCommand.add(c);
            repaint();
        }
        
        public void clear() {
            this.drawPathCommand.clear();
        }
    

}
