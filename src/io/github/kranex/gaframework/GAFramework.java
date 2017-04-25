/*
 *  A Framework for writing Genetic Algorithms in Javascript using data stored in a Derby database.
 *  Copyright (C) 1997 Oliver Strik
 *	
 *  This file is part of GAFramework
 *
 *  GAFramework is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  GAFramework is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with GAFramework.  If not, see <http://www.gnu.org/licenses/>.
 */

/* TODO Problems/other
 * Probabilities for breeding/mutation/other maybe required in db
 * 
 */

package io.github.kranex.gaframework;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * @author oli
 *
 */
public class GAFramework {

	/* constants for debugging and verbose output. */
	public static final boolean VERBOSE = true;
	public static final boolean DEBUG = true;

	/* declarations for the javascript engine. */
	private static ScriptEngine engine;
	private static Invocable inv;

	/* declaration for the database. */
	private static Connection database;

	/* initialization of the framework script break boolean. */
	private boolean BREAK = false;

	/* start of the java program. */
	public static void main(String[] args) throws NumberFormatException, FileNotFoundException, ScriptException,
			ClassNotFoundException, SQLException, NoSuchMethodException {
		System.out.println("GAFramework  Copyright (C) 2017 Oliver Strik");
		System.out.println("This program comes with ABSOLUTELY NO WARRANTY; for details type `GAFramework -w'.");
		System.out.println(
				"This is free software, and you are welcome to redistribute it under certain conditions; type `GAFramework -c' for details.\n");
		switch (args[0]) {
		case "-w":
			System.out.println(
					"This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.");
			return;
		case "-c":
			System.out.println(
					"This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.");
			return;
		case "-h":
			printHelp();
			return;
		default:
			break;
		}
		try {
			/* initialize database. */
			database = createDatabaseConnection(args[1]);

			/* starts the GASolver program. */
			new GAFramework(new File(args[0]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
			
			debug("closing database connection...");
			/* shuts down the database. */
			database.close();
		} catch (ArrayIndexOutOfBoundsException e) {
			/* this happens if not enough arguments are given to the program. */
			System.out.println("[ERROR] Not enough arguments. Usage:");
			printHelp();
		}
	}

	/** 
	 * Creates a database connection object.
	 * Loads the embedded driver, starts and connects to database using the connection URL
	 * Generated via the given database folder.
	 * 
	 * @param db the database folder
	 * @return
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public static Connection createDatabaseConnection(String db) throws SQLException, ClassNotFoundException {
		String driver = "org.apache.derby.jdbc.EmbeddedDriver";
		Class.forName(driver);
		String url = "jdbc:derby:" + db;
		return DriverManager.getConnection(url);
	}

	/**
	 * Start of the GAFramework Program.
	 * 
	 * @param file
	 *            javascript genetic script.
	 * @param itter
	 *            # of iterations.
	 * @param poolSize
	 *            # of chromosomes in the pool.
	 * @throws FileNotFoundException
	 * @throws ScriptException
	 * @throws SQLException
	 * @throws NoSuchMethodException
	 */
	public GAFramework(File file, int itter, int poolSize)
			throws FileNotFoundException, ScriptException, SQLException, NoSuchMethodException {
		
		/* calls the javascript initalisation method. */
		initScriptEngine(file);
		
		/* init method not explicitly required, so try it and ignore if nothing happens. */
		try {
			inv.invokeFunction("init");
		} catch (NoSuchMethodException e) {
			debug("no init method...");
		}
		
		/* invoke the initPool function. */
		inv.invokeFunction("initPool", poolSize);
		
		for (int i = 0; i < itter; i++) {
			/* invoke breed and mutate functions, also break if the script calls for it */
			inv.invokeFunction("breed");
			inv.invokeFunction("mutate");
			if (BREAK) {
				break;
			}
		}
		/* invoke the output function to output the final solution or other stuff as required */
		inv.invokeFunction("output");
		
		// Statement statement = database.createStatement();
		// ResultSet table = statement.executeQuery("SELECT * FROM CITIES");
		// table.get
	}

	/**
	 * javascript engine init method.
	 * 
	 * @param file
	 *            javascript genetic script.
	 * @throws FileNotFoundException
	 * @throws ScriptException
	 */
	private void initScriptEngine(File file) throws FileNotFoundException, ScriptException {
		debug("init script engine");
		ScriptEngineManager sem = new ScriptEngineManager();
		engine = sem.getEngineByName("JavaScript");
		
		/* add the variables BREAK and database to the Scripts global variables. 
		 * This is a link of sorts. changes made here affect the variables in 
		 * the Script, changes to the variables in the Script affect the variables
		 * here.*/
		engine.put("BREAK", BREAK);
		engine.put("db", database);
		
		engine.eval(new FileReader(file));
		inv = (Invocable) engine;
	}

	/**
	 * Debug logger.
	 * 
	 * @param string
	 *            debug message to output.
	 */
	private static void debug(String string) {
		if (DEBUG) {
			System.out.println("[DEBUG] " + string);
		}
	}

	/**
	 * Verbose logger.
	 * 
	 * @param string
	 *            message to output.
	 */
	@SuppressWarnings("unused")
	private static void verbose(String string) {
		if (VERBOSE) {
			System.out.println(string);
		}
	}

	/**
	 * Outputs the Help Text.
	 */
	public static void printHelp() {
		System.out.println("GAFramework <Framework Script> <Database> <Generations> <Chromosomes/Pool>");
	}
}