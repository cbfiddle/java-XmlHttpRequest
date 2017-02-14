/*
 * $Id: XmlHttpRequest.java 158 2006-12-20 01:28:38Z rbair $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle, Santa Clara, California 95054, U.S.A. All rights
 * reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to
 * the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package org.jdesktop.http.async;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.jdesktop.dom.SimpleDocument;
import org.jdesktop.dom.SimpleDocumentBuilder;
import org.jdesktop.http.Method;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Mimics the AJAX equivilent. The idea is that this class will
 * make a request and get as a response some valid XML.
 * 
 * @author rbair
 */
public class XmlHttpRequest extends AsyncHttpRequest {

	//responseXML: DOM-compatible document object of data returned from server process
	private SimpleDocument responseXML;

	/** Creates a new instance of XmlHttpRequest */
	public XmlHttpRequest() {
	}

	/**
	 * If the readyState attribute has a value other than LOADED, then this method
	 * will return null. Otherwise, if the Content-Type contains text/xml, application/xml,
	 * or ends in +xml then a Document will be returned. Otherwise, null is returned.
	 */
	public final SimpleDocument getResponseXML() {
		if (getReadyState() == ReadyState.LOADED) {
			return responseXML;
		}
		else {
			return null;
		}
	}

	@Override
	protected void reset() {
		setResponseXML(null);
		super.reset();
	}

	@Override
	protected void handleResponse(String responseText) throws Exception {
		if (responseText == null) {
			setResponseXML(null);
		}
		else {
			try {
				setResponseXML(SimpleDocumentBuilder.simpleParse(responseText));
			}
			catch (Exception e) {
				setResponseXML(null);
				throw e;
			}
		}
	}

	private void setResponseXML(SimpleDocument dom) {
		Document old = this.responseXML;
		this.responseXML = dom;
		firePropertyChange("responseXML", old, this.responseXML);
	}

	public static void printDocument(Document doc, OutputStream out) throws IOException, TransformerException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

		transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(out, "UTF-8")));
	}

	public static void main(String[] args) {
		final XmlHttpRequest req = new XmlHttpRequest();
		req.addReadyStateChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() == ReadyState.LOADED) {
					System.out.println("LOADED");
					System.out.println(req.getStatus());
					System.out.println(req.getStatusText());
					System.out.println(req.getException());
					System.out.println(req.getResponseText());
					Document dom = req.getResponseXML();
					XPath xpath = XPathFactory.newInstance().newXPath();
					try {
						XPathExpression exp = xpath.compile(
								"/Envelope/Body/GetCitiesByCountryResponse/GetCitiesByCountryResult");
						NodeList nodes = (NodeList) exp.evaluate(dom, XPathConstants.NODESET);
						for (int i = 0; i < nodes.getLength(); i++) {
							System.out.println(nodes.item(i).getTextContent());
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
				else if (evt.getNewValue() == ReadyState.RECEIVING) {
					System.out.println("RECEIVING");
					System.out.println(req.getResponseText());
					Document dom = req.getResponseXML();
					try {
						printDocument(dom, System.out);
					}
					catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
		});
		try {
			req.open(Method.GET, "http://www.webservicex.net/globalweather.asmx");
			req.setRequestHeader("Content-Type", "text/xml; charset=utf-8");
			req.send(
					"<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><GetCitiesByCountry xmlns=\"http://www.webserviceX.NET\"><CountryName>US</CountryName></GetCitiesByCountry>  </soap:Body></soap:Envelope>");
			synchronized (req) {
				req.wait(20000);
			}

		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}