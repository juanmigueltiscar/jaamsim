/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2004-2011 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.sandwell.JavaSimulation3D;

import com.jaamsim.ui.FrameBox;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation.Util;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputListener;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * Class to display information about model objects. <br>
 * displays the results of getInfo for an object
 */
public class PropertyBox extends FrameBox {

	private static PropertyBox myInstance;  // only one instance allowed to be open
	private Entity currentEntity;
	private int presentPage;
	private final IntegerVector presentLineByPage = new IntegerVector();
	private final JTabbedPane jTabbedFrame = new JTabbedPane();
	private boolean ignoreStateChange = false;

	public PropertyBox() {
		super( "Property Viewer" );
		setDefaultCloseOperation(FrameBox.HIDE_ON_CLOSE);

		jTabbedFrame.setPreferredSize( new Dimension( 800, 400 ) );

		// Register a change listener
		jTabbedFrame.addChangeListener(new ChangeListener() {

			// This method is called whenever the selected tab changes
			@Override
			public void stateChanged(ChangeEvent evt) {

				if( ! ignoreStateChange ) {
					presentPage = jTabbedFrame.getSelectedIndex();
					updateValues();
				}
			}
		});

		getContentPane().add( jTabbedFrame );
		setSize( 300, 150 );
		setLocation(0, 110);

		pack();
	}

	private JScrollPane getPropTable() {
		final JTable propTable = new AjustToLastColumnTable(0, 3);

		int totalWidth = jTabbedFrame.getWidth();
		propTable.getColumnModel().getColumn( 2 ).setWidth( totalWidth / 2 ); // 50%
		propTable.getColumnModel().getColumn( 1 ).setWidth( totalWidth / 5 ); // 20%
		propTable.getColumnModel().getColumn( 0 ).setWidth( 3 * totalWidth / 10 ); // 30%

		propTable.getColumnModel().getColumn( 0 ).setHeaderValue( "Property" );
		propTable.getColumnModel().getColumn( 1 ).setHeaderValue( "Type" );
		propTable.getColumnModel().getColumn( 2 ).setHeaderValue( "Value" );

		propTable.getColumnModel().getColumn(0).setCellRenderer(FrameBox.colRenderer);
		propTable.getColumnModel().getColumn(1).setCellRenderer(FrameBox.colRenderer);
		propTable.getColumnModel().getColumn(2).setCellRenderer(FrameBox.colRenderer);

		propTable.getTableHeader().setFont(FrameBox.boldFont);
		propTable.getTableHeader().setReorderingAllowed(false);

		propTable.addMouseListener( new MouseInputListener() {
			@Override
			public void mouseEntered( MouseEvent e ) {

			}
			@Override
			public void mouseClicked( MouseEvent e ) {

				// Left double click
				if( e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1 ) {

					// Get the index of the row the user has clicked on
					int row = propTable.getSelectedRow();
					int column = propTable.getSelectedColumn();
					String name =  propTable.getValueAt(row, column).toString();

					// Remove HTML
					name = name.replaceAll( "<html>", "").replace( "<p>", "" ).replace( "<align=top>", "" ).replace("<br>", "" ).trim();
					Entity entity = Input.tryParseEntity(name, Entity.class);

					// An Entity Name
					if( entity != null ) {
						FrameBox.setSelectedEntity(entity);
					}
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {}
			@Override
			public void mouseMoved(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mouseDragged(MouseEvent e) {}

			@Override
			public void mousePressed( MouseEvent e ) {

				// Right Mouse Button
				if( e.getButton() == MouseEvent.BUTTON3 ) {

					// get the coordinates of the mouse click
					Point p = e.getPoint();

					// get the row index that contains that coordinate
					int row = propTable.rowAtPoint( p );

					// Not get focused on the cell
					if( row < 0 ) {
						return;
					}
					String type =  propTable.getValueAt(row, 1).toString();

					// Remove HTML
					type = type.replaceAll( "<html>", "").replaceAll( "<p>", "" ).replaceAll( "<align=top>", "" ).replaceAll("<br>", "" ).trim();

					// A list of entities
					if( type.equalsIgnoreCase("Vector") || type.equalsIgnoreCase("ArrayList")) {

						// Define a popup menu
						JPopupMenu popupMenu = new JPopupMenu( );
						ActionListener actionListener = new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								Entity entity = Input.tryParseEntity(e.getActionCommand(), Entity.class);
								FrameBox.setSelectedEntity(entity);
							}
						};

						// Split the values in the list
						String value =  propTable.getValueAt(row, 2).toString();
						value = value.replaceAll( "<html>", "").replaceAll( "<p>", "" ).replaceAll( "<align=top>", "" ).replaceAll("<br>", "" ).trim();
						String[] items = value.split( " " );
						for( int i = 0; i < items.length; i++ ) {
							String each = items[ i ].replace( ",", "");
							each = each.replace("[", "");
							each = each.replace("]", "");
							Entity entity = Input.tryParseEntity(each, Entity.class);

							// This item is an entity
							if( entity != null ) {

								// Add the entity to the popup menu
								JMenuItem item = new JMenuItem( entity.getName() );
								item.addActionListener( actionListener );
								popupMenu.add( item );
							}
						}

						// Show the popup menu if it has items
						if( popupMenu.getComponentCount() > 0 ) {
							popupMenu.show( e.getComponent(), e.getX(), e.getY() );
						}
					}
				}
			}
		});

		JScrollPane jScrollPane = new JScrollPane(propTable);
		return jScrollPane;
	}

	@Override
	public void setEntity( Entity entity ) {
		if(currentEntity == entity || ! this.isVisible())
			return;

		// Prevent stateChanged() method of jTabbedFrame to reenter this method
		if( ignoreStateChange ) {
			return;
		}
		ignoreStateChange = true;

		if (entity == null) {
			setTitle("Property Viewer");
			presentPage = 0;
			currentEntity = entity;
			jTabbedFrame.removeAll();
			ignoreStateChange = false;
			return;
		}

		ArrayList<ClassFields> cFields = getFields(entity);

		// A different class has been selected, or no previous value for currentEntity
    	if(currentEntity == null || currentEntity.getClass() != entity.getClass()) {

		presentPage = 0;
		presentLineByPage.clear();

    		// Remove the tabs for new selected entity
    		jTabbedFrame.removeAll();
    	}
    	currentEntity = entity;

		// Create tabs if they do not exist yet
		if( jTabbedFrame.getTabCount() == 0 ) {
			for( int i = 0; i < cFields.size(); i++ ) {
				// The properties in the current page
				ClassFields cf = cFields.get(i);
				jTabbedFrame.addTab(cf.klass.getSimpleName(), getPropTable());

				JTable propTable = (JTable)(((JScrollPane)jTabbedFrame.getComponentAt(i)).getViewport().getComponent(0));
				((javax.swing.table.DefaultTableModel)propTable.getModel()).setRowCount(cf.fields.size());

				// populate property and type columns for this propTable
				for( int j = 0; j < cf.fields.size(); j++ ) {
					propTable.setValueAt(cf.fields.get(j).getName(), j, 0);
					propTable.setValueAt(cf.fields.get(j).getType().getSimpleName(), j, 1);
				}
			}
		}
		ignoreStateChange = false;

		updateValues();
	}

	@Override
	public void updateValues() {
		if(currentEntity == null || ! this.isVisible())
			return;

		setTitle(String.format("Property Viewer - %s", currentEntity));

		ClassFields cf = getFields(currentEntity).get( presentPage );

		JTable propTable = (JTable)(((JScrollPane)jTabbedFrame.getComponentAt(presentPage)).getViewport().getComponent(0));

		// Print value column for current page
		for( int i = 0; i < cf.fields.size(); i++ ) {
			String[] record = getField(currentEntity,  cf.fields.get(i)).split( "\t" );

			// Value column
			if( record.length > 2 ) {

				// Number of items for an appendable property
				int numberOfItems = 1;
				for( int l = 0; l < record[2].length() - 1 ; l++ ) {

					// Items are separated by "},"
					if( record[2].substring( l, l + 2 ).equalsIgnoreCase( "},") ) {
						numberOfItems++;
					}
				}

				// If the property is not appendable, then check for word wrapping
				if( numberOfItems == 1 ) {

					// Set the row height for this entry
					int stringLengthInPixel = Util.getPixelWidthOfString_ForFont( record[2].trim(), propTable.getFont() );
					double lines = (double) stringLengthInPixel / ((double) propTable.getColumnModel().getColumn( 2 ).getWidth() );

					// Adjustment
					if( lines > 1 ){
						lines *=  1.1 ;
					}
					int numberOfLines = (int) Math.ceil( lines );
					String lineBreaks = "";

					// Only if there are not too many lines
					if( numberOfLines < 10 ) {
						int height = (numberOfLines - 1 ) * (int) (propTable.getRowHeight() * 0.9)  + propTable.getRowHeight();  // text height is about 90% of the row height
						propTable.setRowHeight( i, height );

						// Add extra line breaks to the end of the string to shift the entry to the top of the row
						// (Jtable should do this with <align=top>, but this does not work)
						for( int j = 0; j < numberOfLines; j++ ) {
							lineBreaks = lineBreaks + "<br>";
						}

						// Set the value
						String value = "null";
						if( ! record[2].equalsIgnoreCase( "<null>" ) ) {
							value = record[2];
						}
						propTable.setValueAt( "<html> <p> <align=top>" + " " + value + lineBreaks, i, 2 );
					}
					else {
						propTable.setValueAt( numberOfLines + " rows is too large to be displayed!", i, 2 );
					}
				}

				// If the property is appendable, then add line breaks after each item
				else if( numberOfItems <= 50 ) {

					// Set the row height for this entry
					int height = (numberOfItems - 1 ) * (int) (propTable.getRowHeight() * 0.9)  + propTable.getRowHeight(); // text height is about 90% of the row height
					propTable.setRowHeight( i, height );

					// Set the entry
					String str = record[2].replaceAll( "},", "}<br>" );  // Replace the brace and comma with a brace and line break
					propTable.setValueAt( "<html>" + str, i, 2 );
				}

				// Do not print an appendable entry that requires too many rows
				else {
					propTable.setValueAt( numberOfItems + " rows is too large to be displayed!", i, 2 );
				}
			}
    	}
	}

	/**
	 * Returns the only instance of the property box
	 */
	public synchronized static PropertyBox getInstance() {
		if (myInstance == null)
			myInstance = new PropertyBox();

		return myInstance;
	}

	@Override
	public void dispose() {
		myInstance = null;
		super.dispose();
	}

	private static String getField(Object obj, java.lang.reflect.Field field) {
		StringBuilder fieldString = new StringBuilder();

		try {
			// save the field's accessibility, reset after reading
			boolean accessible = field.isAccessible();
			field.setAccessible(true);

			// get the name and type for the property
			fieldString.append(field.getName());
			fieldString.append("\t");
			fieldString.append(field.getType().getSimpleName());
			fieldString.append("\t");
			fieldString.append(propertyFormatObject(field.get(obj)));

			// set the access for this field to what it originally was
			field.setAccessible(accessible);
		}
		catch( SecurityException e ) {
			System.out.println( e );
		}
		catch( IllegalAccessException e ) {
			System.out.println( e );
		}
		return fieldString.toString();
	}

private static class ClassFields implements Comparator<Field> {
	final Class<?> klass;
	final ArrayList<Field> fields;

	ClassFields(Class<?> aKlass) {
		klass = aKlass;

		Field[] myFields = klass.getDeclaredFields();
		fields = new ArrayList<Field>(myFields.length);
		for (Field each : myFields) {
			String name = each.getName();
			// Static variables are all capitalized (ignore them)
			if (name.toUpperCase().equals(name))
				continue;

			fields.add(each);
		}
		Collections.sort(fields, this);
	}

	@Override
	public int compare(Field f1, Field f2) {
		return f1.getName().compareToIgnoreCase(f2.getName());
	}
}

	private static ArrayList<ClassFields> getFields(Entity object) {
		if (object == null)
			return new ArrayList<ClassFields>(0);

		return getFields(object.getClass());
	}

	private static ArrayList<ClassFields> getFields(Class<?> klass) {
		if (klass == null || klass.getSuperclass() == null)
			return new ArrayList<ClassFields>();

		ArrayList<ClassFields> cFields = getFields(klass.getSuperclass());
		cFields.add(new ClassFields(klass));
		return cFields;
	}

	private static String propertyFormatObject( Object value ) {
		if (value == null)
			return "<null>";

		if (value instanceof Entity)
			return ((Entity)value).getInputName();

		if (value instanceof double[])
			return Arrays.toString((double[])value);

		try {
			return value.toString();
		}
		catch (ConcurrentModificationException e) {
			return "";
		}
	}
}
