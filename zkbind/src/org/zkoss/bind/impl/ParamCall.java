package org.zkoss.bind.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;


import org.zkoss.bind.BindContext;
import org.zkoss.bind.Binder;
import org.zkoss.bind.annotation.SelectorParam;
import org.zkoss.bind.annotation.ContextParam;
import org.zkoss.bind.annotation.ContextType;
import org.zkoss.bind.annotation.CookieParam;
import org.zkoss.bind.annotation.Default;
import org.zkoss.bind.annotation.ExecutionArgParam;
import org.zkoss.bind.annotation.ExecutionParam;
import org.zkoss.bind.annotation.HeaderParam;
import org.zkoss.bind.annotation.Param;
import org.zkoss.bind.annotation.QueryParam;
import org.zkoss.bind.annotation.Scope;
import org.zkoss.bind.annotation.ScopeParam;
import org.zkoss.lang.Classes;
import org.zkoss.util.logging.Log;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.select.Selectors;
/**
 * to help invoke a method with {@link Param} etc.. features
 * @author dennis
 *
 */
public class ParamCall {

	private static final Log _log = Log.lookup(ParamCall.class);
	
	private Map<Class<? extends Annotation>, ParamResolver<Annotation>> _paramResolvers;
	private List<Type> _types;//to map class type directly, regardless the annotation
	private boolean _mappingType;//to enable the map class type without annotation, it is for compatible to rc2, only support BindeContext and Binder
	private Map<ContextType,Object> _contextObjects = new HashMap<ContextType,Object>();
	
	private static final String COOKIE_CACHE = "$PARAM_COOKIES$";
	
	private static Component _root = null;
	
	public ParamCall(){
		this(true);
	}
	public ParamCall(boolean mappingType){
		_paramResolvers = new HashMap<Class<? extends Annotation>, ParamResolver<Annotation>>();
		_types = new ArrayList<Type>();
		_mappingType = mappingType;
		_paramResolvers.put(ContextParam.class, new ParamResolver<Annotation>() {
			@Override
			public Object resolveParameter(Annotation anno) {
				return _contextObjects.get(((ContextParam) anno).value());
			}
		});
	}
	
	public void setBindContext(BindContext ctx){
		_types.add(new Type(ctx.getClass(),ctx));
		_contextObjects.put(ContextType.BIND_CONTEXT, ctx);
	}
	public void setBinder(Binder binder){
		_types.add(new Type(binder.getClass(),binder));
		_contextObjects.put(ContextType.BINDER, binder);
		_root = binder.getView();
	}
	
	public void setBindingArgs(final Map<String, Object> bindingArgs){
		_paramResolvers.put(Param.class, new ParamResolver<Annotation>() {
			@Override
			public Object resolveParameter(Annotation anno) {
				return bindingArgs.get(((Param) anno).value());
			}
		});
	}
	
	
	public void call(Object base, Method method) {
		Class<?>[] paramTypes = method.getParameterTypes();
		java.lang.annotation.Annotation[][] parmAnnos = method.getParameterAnnotations();
		Object[] params = new Object[paramTypes.length];

		try {
			for (int i = 0; i < paramTypes.length; i++) {
				params[i] = resolveParameter(parmAnnos[i],paramTypes[i]);
			}
			
			method.invoke(base, params);
		} catch (Exception e) {
			_log.error(e);
			throw UiException.Aide.wrap(e);
		}
	}
	
	private Object resolveParameter(java.lang.annotation.Annotation[] parmAnnos, Class<?> paramType){
		Object val = null;
		boolean hitResolver = false;
		Default defAnno = null;
		for(Annotation anno:parmAnnos){
			Class<?> annotype = anno.annotationType();
			
			if(defAnno==null && annotype.equals(Default.class)){
				defAnno = (Default)anno;
				continue;
			}
			ParamResolver<Annotation> resolver = _paramResolvers.get(annotype);
			if(resolver==null) continue;
			hitResolver = true;
			val = resolver.resolveParameter(anno);
			if(val!=null) {
				val = Classes.coerce(paramType, val);
				break;
			}
			//don't break until get a value
		}
		if(val == null && defAnno != null){
			val = Classes.coerce(paramType, defAnno.value());
		}
		
		//to compatible to rc2, do we have to?
		if(_mappingType && val==null && !hitResolver && _types!=null){
			for (Type type : _types) {
				if (type != null && paramType.isAssignableFrom(type.clz)) {
					val = type.value;
					break;
				}
			}
		}
		return val;
	}
	
	//utility to hold implicit class and runtime value
	private static class Type{
		final Class<?> clz;
		final Object value;
		public Type(Class<?> clz,Object value){
			this.clz = clz;
			this.value = value;
		}
	}
	
	private interface ParamResolver<T> {
		public Object resolveParameter(T anno);
	}

	
	public void setComponent(final Component comp) {
		//scope param
		_paramResolvers.put(ScopeParam.class, new ParamResolver<Annotation>() {
			@Override
			public Object resolveParameter(Annotation anno) {
				final String name = ((ScopeParam)anno).value();
				Scope[] ss = ((ScopeParam)anno).scopes();
				List<Scope> scopes = new ArrayList<Scope>();  
				for(Scope s:ss){
					switch(s){
					case DEFAULT:
						scopes.addAll(Scope.getDefaultScopes());
						break;
					case ALL:
						scopes.addAll(Scope.getAllScopes());
						break;
					default:
						scopes.add(s);
					}
				}
				Object val = null;
				for(Scope scope:scopes){
					final String scopeName = scope.getName();
					Object scopeObj = Components.getImplicit(comp, scopeName);
					if(scopeObj instanceof Map){
						val = ((Map<?,?>)scopeObj).get(name);
						if(val!=null) break;
					}else if(scopeObj !=null){
						_log.error("the scope of "+scopeName+" is not a Map, is "+scopeObj);
					}
				}
				return val;
			}
		});
		
		//component
		_paramResolvers.put(SelectorParam.class, new ParamResolver<Annotation>() {
			@Override
			public Object resolveParameter(Annotation anno) {
				String selector = ((SelectorParam) anno).value();
				boolean local =  ((SelectorParam) anno).local();
				int index =  ((SelectorParam) anno).index();
				if(!local && _root==null){
					return null;
				}
				if(index<0){
					return Selectors.find(local?comp:_root, selector);
				}else{
					return Selectors.find(local?comp:_root, selector, index);
				}
			}
		});
	}
	public void setExecution(final Execution exec) {
		//http param
		_paramResolvers.put(QueryParam.class, new ParamResolver<Annotation>() {
			@Override
			public Object resolveParameter(Annotation anno) {
				return exec.getParameter(((QueryParam) anno).value());
			}
		});
		_paramResolvers.put(HeaderParam.class, new ParamResolver<Annotation>() {
			@Override
			public Object resolveParameter(Annotation anno) {
				return exec.getHeader(((HeaderParam) anno).value());
			}
		});
		_paramResolvers.put(CookieParam.class, new ParamResolver<Annotation>() {
			@Override
			@SuppressWarnings("unchecked")
			public Object resolveParameter(Annotation anno) {
				Map<String,Object> m = (Map<String,Object>)exec.getAttribute(COOKIE_CACHE);
				if(m==null){
					final Object req  = exec.getNativeRequest();
					m = new HashMap<String,Object>();
					exec.setAttribute(COOKIE_CACHE, m);
					
					if(req instanceof HttpServletRequest){
						final Cookie[] cks = ((HttpServletRequest)req).getCookies();
						if(cks != null){
							for(Cookie ck:cks){
								m.put(ck.getName().toLowerCase(), ck.getValue());
							}
						}
					}else/* if(req instanceof PortletRequest)*/{
						//no cookie in protlet 1.0
					}
				}
				return m==null?null:m.get(((CookieParam) anno).value().toLowerCase());
			}
		});

		//execution
		_paramResolvers.put(ExecutionParam.class, new ParamResolver<Annotation>() {
			@Override
			public Object resolveParameter(Annotation anno) {
				return exec.getAttribute(((ExecutionParam) anno).value());
			}
		});
		
		_paramResolvers.put(ExecutionArgParam.class, new ParamResolver<Annotation>() {
			@Override
			public Object resolveParameter(Annotation anno) {
				return exec.getArg().get(((ExecutionArgParam) anno).value());
			}
		});
	}
}