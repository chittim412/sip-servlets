/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mobicents.servlet.sip.core.session;

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;
import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSessionAttributeListener;
import javax.servlet.sip.SipApplicationSessionBindingEvent;
import javax.servlet.sip.SipApplicationSessionBindingListener;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.URI;

import org.mobicents.servlet.sip.startup.SipContext;


public class SipApplicationSessionImpl implements SipApplicationSession {

	private static long DEFAULT_LIFETIME = 1000*60*60;
	
	public static final String SIP_APPLICATION_KEY_PARAM_NAME = "org.mobicents.servlet.sip.ApplicationSessionKey"; 
	
//	private SipListenersHolder listeners;
	
	private Map<String, Object> sipApplicationSessionAttributeMap;

	private Map<SipSessionKey,SipSessionImpl> sipSessions;
	
	private Map<String, HttpSession> httpSessions;
	
	private SipApplicationSessionKey key;	
	
	private long lastAccessTime;
	
	private long creationTime;
	
	private long expirationTime;
	
	private Map<String, ServletTimer> servletTimers;
	
	private boolean valid;

	/**
	 * The first sip application for subsequent requests.
	 */
	private SipContext sipContext;
	
//	private TimerListener agregatingListener;
//	private ArrayList<ServletTimer> runningTimers;	
	
	/**
	 * Passed as info object into Servelt timer that ticks for this sip app
	 * session as expiration timer
	 */
//	private Serializable endObject;					
	
	public SipApplicationSessionImpl(SipApplicationSessionKey key ) {
		sipApplicationSessionAttributeMap = new ConcurrentHashMap<String,Object>() ;
		sipSessions = new ConcurrentHashMap<SipSessionKey,SipSessionImpl>();
		httpSessions = new ConcurrentHashMap<String,HttpSession>();
		servletTimers = new ConcurrentHashMap<String, ServletTimer>();
		this.key = key;
		lastAccessTime = creationTime = System.currentTimeMillis();
		expirationTime = lastAccessTime + DEFAULT_LIFETIME;
		valid = true;		
		//FIXME create and start a timer for session expiration
	}
	
	protected void addSipSession( SipSessionImpl sipSessionImpl) {
		this.sipSessions.put(sipSessionImpl.getKey(), sipSessionImpl);
//		sipSessionImpl.setSipApplicationSession(this);
	}
	
	protected SipSessionImpl removeSipSession (SipSessionImpl sipSessionImpl) {
		return this.sipSessions.remove(sipSessionImpl.getKey());
	}
	
	public void addHttpSession( HttpSession httpSession) {
		this.httpSessions.put(httpSession.getId(), httpSession);
	}
	
	public HttpSession removeHttpSession (HttpSession httpSession) {
		return this.httpSessions.remove(httpSession.getId());
	}
	
	public HttpSession findHttpSession (HttpSession httpSession) {
		return this.httpSessions.get(httpSession.getId());
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSession#encodeURI(javax.servlet.sip.URI)
	 */
	public void encodeURI(URI uri) {
		uri.setParameter("org.mobicents.servlet.sip.ApplicationSessionKey", getId());
	}

	/**
	 * Adds a get parameter to the URL like this:
	 * http://hostname/link -> http://hostname/link?org.mobicents.servlet.sip.ApplicationSessionKey=0
	 * http://hostname/link?something=1 -> http://hostname/link?something=1&org.mobicents.servlet.sip.ApplicationSessionKey=0
	 */
	public URL encodeURL(URL url) {
		String urlStr = url.toExternalForm();
		try
		{
			URL ret;
			if(urlStr.contains("?"))
			{
				ret = new URL(
						url + "&"+ SIP_APPLICATION_KEY_PARAM_NAME + "="
							+ getId().toString());
			}
			else
			{
				ret = new URL(
						url + "?"+ SIP_APPLICATION_KEY_PARAM_NAME + "="
							+ getId().toString());
			}
			return ret;
		}
		catch (Exception e)
		{
			throw new RuntimeException("Failed encoding URL", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSession#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String name) {
		return this.sipApplicationSessionAttributeMap.get(name);
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSession#getAttributeNames()
	 */
	public Iterator<String> getAttributeNames() {
		return this.sipApplicationSessionAttributeMap.keySet().iterator();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSession#getCreationTime()
	 */
	public long getCreationTime() {
		return creationTime;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSession#getExpirationTime()
	 */
	public long getExpirationTime() {
		return expirationTime;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSession#getId()
	 */
	public String getId() {
		return key.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSession#getLastAccessedTime()
	 */
	public long getLastAccessedTime() {
		return lastAccessTime;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSession#getSessions()
	 */
	public Iterator<?> getSessions() {
		return sipSessions.entrySet().iterator();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSession#getSessions(java.lang.String)
	 */
	public Iterator<?> getSessions(String protocol) {
		if("SIP".equalsIgnoreCase(protocol))
			return sipSessions.values().iterator();
		else 
			//sipContext.getManager().findSessions()
			return null;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSession#getSipSession(java.lang.String)
	 */
	public SipSession getSipSession(String id) {
		return sipSessions.get(id);
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSession#getTimers()
	 */
	public Collection<ServletTimer> getTimers() {
		return servletTimers.values();
	}

	/**
	 * Add a servlet timer to this application session
	 * @param servletTimer the servlet timer to add
	 */
	public void addServletTimer(ServletTimer servletTimer){
		servletTimers.put(servletTimer.getId(), servletTimer);
	}
	/**
	 * Remove a servlet timer from this application session
	 * @param servletTimer the servlet timer to remove
	 */
	public void removeServletTimer(ServletTimer servletTimer){
		servletTimers.remove(servletTimer);
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSession#invalidate()
	 */
	public void invalidate() {
		for(SipSessionImpl session: sipSessions.values())
		{
			if(session.isValid())
				throw new IllegalStateException("All SIP " +
						"and HTTP sessions must be invalidated" +
						" before invalidating the application session.");
		}
		valid = false;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSession#isValid()
	 */
	public boolean isValid() {
		return valid;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSession#removeAttribute(java.lang.String)
	 */
	public void removeAttribute(String name) {

		if (!isValid())
			throw new IllegalStateException(
					"Can not bind object to session that has been invalidated!!");

		if (name == null)
			// throw new NullPointerException("Name of attribute to bind cant be
			// null!!!");
			return;

		SipApplicationSessionBindingEvent event = new SipApplicationSessionBindingEvent(
				this, name);
		SipListenersHolder listeners = sipContext.getListeners();
		for (SipApplicationSessionBindingListener listener : listeners
				.getSipApplicationSessionBindingListeners()) {
			listener.valueUnbound(event);
		}

		for (SipApplicationSessionAttributeListener listener : listeners
				.getSipApplicationSessionAttributeListeners()) {
			listener.attributeRemoved(event);
		}

		this.sipApplicationSessionAttributeMap.remove(name);
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSession#setAttribute(java.lang.String, java.lang.Object)
	 */
	public void setAttribute(String key, Object attribute) {

		if (!isValid())
			throw new IllegalStateException(
					"Can not bind object to session that has been invalidated!!");

		if (key == null)
			throw new NullPointerException(
					"Name of attribute to bind cant be null!!!");
		if (attribute == null)
			throw new NullPointerException(
					"Attribute that is to be bound cant be null!!!");

		SipApplicationSessionBindingEvent event = new SipApplicationSessionBindingEvent(
				this, key);
		SipListenersHolder listeners = sipContext.getListeners();
		if (sipApplicationSessionAttributeMap.containsKey(key)) {
			// This is initial, we need to send value bound event						
			for (SipApplicationSessionBindingListener listener : listeners
					.getSipApplicationSessionBindingListeners()) {
				listener.valueBound(event);

			}

			for (SipApplicationSessionAttributeListener listener : listeners
					.getSipApplicationSessionAttributeListeners()) {
				listener.attributeAdded(event);
			}

		} else {

			for (SipApplicationSessionAttributeListener listener : listeners
					.getSipApplicationSessionAttributeListeners()) {
				listener.attributeReplaced(event);
			}

		}

		this.sipApplicationSessionAttributeMap.put(key, attribute);

	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSession#setExpires(int)
	 */
	public int setExpires(int deltaMinutes) {
		if(deltaMinutes == 0)
			this.expirationTime = Long.MAX_VALUE;
		else
			this.expirationTime = System.currentTimeMillis() + deltaMinutes*1000*60;
		return 0;
	}

//	public SipListenersHolder getListeners() {
//		return listeners;
//	}
//
//	public void setListeners(SipListenersHolder listeners) {
//		this.listeners = listeners;
//	}

	public boolean hasTimerListener() {
		return this.sipContext.getListeners().getTimerListener() != null;
	}	

	public SipContext getSipContext() {
		return sipContext;
	}

	public void setSipContext(SipContext sipContext) {
		this.sipContext = sipContext;
	}
	
//	public TimerListener getAgregatingListener() {
//		return agregatingListener;
//	}
//
//	public void setAgregatingListener(TimerListener agregatingListener) {
//		this.agregatingListener = agregatingListener;
//	}
//
//	Serializable getEndObject() {
//		if (this.endObject == null)
//			this.endObject = new Serializable() {
//			};
//
//		return this.endObject;
//	}
	
	void expirationTimerFired()
	{
		
	}

	/**
	 * @return the key
	 */
	public SipApplicationSessionKey getKey() {
		return key;
	}

	/**
	 * @param key the key to set
	 */
	public void setKey(SipApplicationSessionKey key) {
		this.key = key;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSession#getApplicationName()
	 */
	public String getApplicationName() {		
		return key.getApplicationName();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipApplicationSession#getTimer(java.lang.String)
	 */
	public ServletTimer getTimer(String id) {
		return servletTimers.get(id);
	}
	
//	public void timerScheduled(ServletTimerImpl st) {
//		
//		this.runningTimers.add(st);
//
//	}

}
