/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kasabi.labs.nerdf;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.apache.velocity.runtime.log.NullLogChute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aliasi.chunk.Chunk;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.base.file.Location;
import com.hp.hpl.jena.util.FileManager;

@SuppressWarnings("serial")
public class NERDFServlet extends HttpServlet {

	private static Logger vlog = LoggerFactory.getLogger("Velocity");
	private static LogChute velocityLog = new NullLogChute();
	private final VelocityEngine velocity = new VelocityEngine();
	private LingPipeLinker linker;

	public NERDFServlet() {
		velocity.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM, velocityLog);
		velocity.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");
		velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "file,class");
		velocity.setProperty("file.resource.loader.class", org.apache.velocity.runtime.resource.loader.FileResourceLoader.class.getName());
		velocity.setProperty("file.resource.loader.path", "src/main/webapp/WEB-INF/classes/templates");
		velocity.setProperty("class.resource.loader.class", org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader.class.getName());
		velocity.setProperty("class.resource.loader.path", "/templates");
		velocity.init();
	}

	@Override
	public void init() throws ServletException {
	    String value = getServletConfig().getInitParameter("tdb.location");
		FileManager fm = FileManager.get();
	    Location location = new Location(value);
	    Dataset dataset = TDBFactory.createDataset(location);
	    Query query = QueryFactory.read("/queries/namedentities.rq", fm, null, Syntax.syntaxSPARQL);
	    QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
	    try {
	    	ResultSet results = qexec.execSelect();
		    linker = new LingPipeLinker ( results );
	    } finally { 
	    	qexec.close(); 
	    }
	}
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) {
		process(req, resp);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) {
		process(req, resp);
	}

	private void process(HttpServletRequest req, HttpServletResponse resp) {
		VelocityContext context = new VelocityContext();
		String path = path(req);
		try {
			Template temp = velocity.getTemplate(path);
			context.put("request", req);
			String text = req.getParameter("text");
			if ( text != null ) {
				context.put("text", text);
				String result = text;
				Set<Chunk> chunks = linker.link(text);
				int offset = 0;
				int prev_start = 0;
				ArrayList<String> types = new ArrayList<String>();
				String link_close = "</a>";
				for (Chunk chunk : chunks) {
					String[] tokens = chunk.type().split("\t");
					if ( chunk.start() != prev_start ) {
						StringBuffer link_open = new StringBuffer();
						link_open.append("<a href=\"");
						link_open.append(tokens[1]);
						link_open.append("\"");
						if ( types.size() > 0 ) {
							link_open.append(" title=\"");
							link_open.append(types.get(0));
							for ( int i=1; i < types.size(); i++ ) {
								link_open.append(", ");
								link_open.append(types.get(i));
							}
							link_open.append("\"");
						}
						link_open.append(">");
						result = result.substring(0, chunk.start() + offset) + link_open + result.substring(chunk.start() + offset, chunk.end() + offset) + link_close + result.substring(chunk.end() + offset);
						offset += link_open.length() + link_close.length();
						prev_start = chunk.start();
						types = new ArrayList<String>();
					} else {
						types.add(tokens[0]);
					}
				}
				context.put("result", result);				
			}

			resp.setCharacterEncoding("UTF-8");
			Writer out = resp.getWriter();
			temp.merge(context, out);
			out.flush();
		} catch (ResourceNotFoundException ex) {
			vlog.error("Resource not found: " + ex.getMessage());
		} catch (ParseErrorException ex) {
			vlog.error("Parse error (" + path + ") : " + ex.getMessage());
		} catch (MethodInvocationException ex) {
			vlog.error("Method invocation exception (" + path + ") : " + ex.getMessage());
		} catch (IOException ex) {
			vlog.warn("IOException", ex);
		}
	}

	private String path(HttpServletRequest request) {
		String path = request.getPathInfo();
		if (path != null)
			return path;
		path = request.getServletPath();
		if (path != null)
			return path;
		return null;
	}

	@Override
	public String getServletInfo() {
		return "Velocity Servlet";
	}

	// taken from Apache Jena - Fuseki. Nice, kudos to AndyS.
	static class SimpleSLF4JLogChute implements LogChute {
		private Logger logger;
		SimpleSLF4JLogChute(Logger log) {
			this.logger = log;
		}

		@Override
		public void init(RuntimeServices rs) throws Exception {}

		@Override
		public void log(int level, String message) {
			if (logger == null)
				return;
			switch (level) {
			case LogChute.TRACE_ID:
				logger.trace(message);
				return;
			case LogChute.DEBUG_ID:
				logger.debug(message);
				return;
			case LogChute.INFO_ID:
				logger.info(message);
				return;
			case LogChute.WARN_ID:
				logger.warn(message);
				return;
			case LogChute.ERROR_ID:
				logger.error(message);
				return;
			}
		}

		@Override
		public void log(int level, String message, Throwable t) {
			if (logger == null)
				return;
			t = null;
			switch (level) {
			case LogChute.TRACE_ID:
				logger.trace(message, t);
				return;
			case LogChute.DEBUG_ID:
				logger.debug(message, t);
				return;
			case LogChute.INFO_ID:
				logger.info(message, t);
				return;
			case LogChute.WARN_ID:
				logger.warn(message, t);
				return;
			case LogChute.ERROR_ID:
				logger.error(message, t);
				return;
			}
		}

		@Override
		public boolean isLevelEnabled(int level) {
			switch (level) {
			case LogChute.TRACE_ID:
				return logger.isTraceEnabled();
			case LogChute.DEBUG_ID:
				return logger.isDebugEnabled();
			case LogChute.INFO_ID:
				return logger.isInfoEnabled();
			case LogChute.WARN_ID:
				return logger.isWarnEnabled();
			case LogChute.ERROR_ID:
				return logger.isErrorEnabled();
			}
			return true;
		}
	}

}
