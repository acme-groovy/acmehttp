/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package groovyx.acme.net;

import junit.framework.TestResult;
import junit.framework.TestCase;

class AcmeHTTPTest extends GroovyTestCase {
	@Override
	void run(TestResult result){
		String testName = null;
		StringBuilder testCode = new StringBuilder(1024);
		int state = 0; //0:default, 1:code
		
		new File("./EXAMPLES.md").eachLine("UTF-8"){line->
			if(state==0){
				if(line.startsWith("###"))testName=line.substring(3);
				else if(line=="```groovy")state=1;
			}else if(state==1){
				if(line=="```"){
					state=0;
					result.run( 
						new TestCase(testName){
							@Override
							protected void runTest(){
								new GroovyShell().evaluate(testCode.toString())
							}
						}
					);
					testCode.setLength(0);
				}else{
					if(testCode.length()>0)testCode.append("\n");
					testCode.append(line);
				}
			}
		}
	}

	public void testFakeMethod(){}
	

}
