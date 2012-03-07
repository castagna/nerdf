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

import java.util.Set;

import org.openjena.atlas.logging.ProgressLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.ExactDictionaryChunker;
import com.aliasi.dict.MapDictionary;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class LingPipeLinker {
	
	private static final Logger log = LoggerFactory.getLogger(LingPipeLinker.class) ;
	
	private MapDictionary<String> dictionary = new MapDictionary<String>();
	private ExactDictionaryChunker chunker;
	
	public LingPipeLinker() {
		log.debug("LingPipeLinker()");
	}

	public LingPipeLinker ( ResultSet rs ) {
		log.debug("LingPipeLinker({})", rs);
		load ( rs );
	}

	public Set<Chunk> link ( String text ) {
		log.debug("link({}...)", text.substring(0, 40));
		
		if ( chunker == null ) {
			chunker = new ExactDictionaryChunker(dictionary, IndoEuropeanTokenizerFactory.INSTANCE, true, false);
		}
		
		Chunking chunking = chunker.chunk(text);
		return chunking.chunkSet();
	}
	
	public void load ( ResultSet rs ) {
		ProgressLogger progress = new ProgressLogger(log, "", 1000, 10000);
		progress.start();
		while ( rs.hasNext() ) {
			QuerySolution qs = rs.next();
			String label = qs.getLiteral("label").getLexicalForm();
			if ( label.length() > 1 ) {
				String uri = qs.getResource("uri").getURI();
				String type = qs.getResource("type").getURI();
				dictionary.addEntry(new DictionaryEntry<String>(label, type + "\t" + uri, new Double(label.length())));
				progress.tick();
			}
		}
		progress.finish();
		log.debug ("load({}), dictionary now has {} entries", rs, dictionary.size());
	}
	
}
