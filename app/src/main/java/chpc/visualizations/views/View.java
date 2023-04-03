package chpc.visualizations.views;

import chpc.dataLoader.DataStore;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import java.awt.*;

public abstract class View extends JFrame {
  public String chartType;

  DataStore dataStore;

  /**
   * create View instance with selected data
   */
  public View() {
    dataStore = DataStore.getInstance();
  }

  // step1 access data

  /**
   * access selected data
   */
  public abstract void addDataset();

  // step2 plot view

  /**
   * @return plotted view based on selected visualization
   */
  public abstract JFreeChart plotView();

  // step3

  /**
   * create a selected visualization
   */
  public void createChart() {
    // 1.get data
    addDataset();

    // 2.plot the view and add it to ChartPanel
    JFreeChart chart = plotView();
    ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setPreferredSize(new Dimension(700, 500));
    chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    chartPanel.setBackground(Color.white);

    // 4.create the main panel for store all component
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout());
    mainPanel.setBackground(Color.LIGHT_GRAY);
    mainPanel.add(chartPanel, BorderLayout.CENTER);

    // 5. add main panel to View frame
    setSize(900, 700);
    setContentPane(mainPanel);
    setVisible(true);
  }
}