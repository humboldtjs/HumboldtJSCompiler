/*
* HumboldtJSDOM
* http://humboldtjs.com/
*
* Copyright (c) 2012 Daniël Haveman
* Licensed under the MIT license
* http://humboldtjs.com/license.html
*/

// Defines the __hjs object which provides most of the functionality for
// classical inheritance in JS and also fixes some internal objects to provide
// the complete functionality of EcmaScript
browser = true;
if (typeof module !== "undefined") {
	window = global;
	browser = false;
}
if (window.__hjs == undefined) {
	__hjs = {};
	__hjs.version = "0.9.7";
	__hjs._inheriting = false;
	__hjs.appId = -1;
	__hjs._instances = [];
	if (window.humboldtReadyHandlers == undefined) window.humboldtReadyHandlers = [];
		
	(function(){
		inc = [];
		pre = [];
		done = [];
		instance = 0;
	
		if([].indexOf==undefined)Array.prototype.indexOf=function(v){for(var i=0;i<this.length;i++)if(this[i]==v)return i;return-1}
		
		__hjs.autoInit = function(c) {
			var localInstance = instance++;
			if (browser)
				document.write("<div id=\"__" + c + "_" + localInstance + "__\"></div>");
			window.humboldtReadyHandlers.push(function(){__hjs.appId = "__" + c + "_" + localInstance + "__";eval("new " + c + "();");__hjs.appId = -1;});
		}
		
		__hjs.app = function(c) {
			inc.push(c);
		}
		
		__hjs.regUid = function(u, c) {
			if (pre.indexOf(c) == -1)
				pre.push(c);
			if (c && u) {
				__hjs["__" + u] = c.prototype;
				__hjs["_$" + u] = c;
			}
		}
		
		__hjs.ready = function(c) {
			if (done.indexOf(c) == -1 && c != null) done.push(c);
			if (done.length == inc.length) {
				__hjs.setupInheritance();
				if (window.humboldtReadyHandlers != undefined) {
					var theHandlers = window.humboldtReadyHandlers;
					window.humboldtReadyHandlers = [];
					for (var i = 0; i < theHandlers.length; i++) {
						theHandlers[i]();
					}
				}
			}
		}
		
		__hjs.include = function(c, e) {
			if (c == null) return;
			if (e == null) e = false;
			if (inc.indexOf(c) == -1) {
				inc.push(c);
				var b = "";
				if (window.humboldtBaseUrl != undefined) b = window.humboldtBaseUrl;
				if (e == true) {
					if (!browser) {
						require("./"+b+c.split(".").join("/")+".js");
					} else {
						var theScript = document.createElement("script");
						theScript.src = b+c.split(".").join("/")+".js";
						var theHead = document.getElementsByTagName("head")[0];
						theHead.appendChild(theScript);
					}
				}
			} else {
				inc.splice(inc.indexOf(c), 1);
				inc.push(c);
			}
		}
		
		__hjs.pkg = function(p) {
			if (p == "") return;
			p = p.split(".");
			var o = window;
			for (var i = 0; i < p.length; i++) {
				if (o[p[i]] == undefined) o[p[i]] = {};
				o = o[p[i]];
			}
			eval(p[0] + "=window[\"" + p[0] +"\"];");
		}
	
		__hjs.setupInheritance = function() {
			var d = [pre, inc];
			for (var j = 0; j < d.length; j++)
			for (var i = d[j].length - 1; i >= 0; i--) {
				var c = eval(d[j][i]);
				if (c && c.prototype && !c.prototype.__inheritanceComplete) {
					var cp = c.prototype;
					if (cp.__chain) cp.__chain.apply();
					var m = cp.__implements;
					if(m)
					for(var k = 0; k < m.length; k++) {
						var l = eval(m[k])["extends"];
						if (l.length > 0) {
							for (var n = 0; n < l.length; n++) {
								if (m.indexOf(l[n]) == -1) m.push(l[n]);
							}
						}
					}
	
					if (c.prototype.__class) {
						c.prototype.__inheritanceComplete = true;
					} else {
						var t = d[j][i];
						d[j].splice(i, 1);
						d[j].unshift(t);
						i++;
					}
				}
			}
		}
		
		__hjs.inherit = function(n, c, s) {
			if (c == null) return true;
			if (!c.prototype.__class) return false;
	
			__hjs._inheriting = true;
			n.prototype = new c();
			__hjs._inheriting = false;
			
			if (s) __hjs.regUid(s, n);

			return true;
		}
		
		__hjs.cls = function(p) {
			p.__class = p;
			p.__methods = p.__methods ? p.__methods.slice() : [];
		}
		
		__hjs.regm = function(p,n,f) {
			p.__methods.push(n);
			p[n] = f;
		}
		
		__hjs.bindMethods = function(s,c) {
			if (s.__bound) return;
			var i;
			for (i = 0; i < c.prototype.__methods.length; i++) {
				var n = c.prototype.__methods[i];
				s[n] = __hjs.bind(s, c.prototype[n]);
			}
			s.__bound = true;
		}
	
		__hjs.event = function(e) {
			e = e ? e : window.event;
			if (e) {
				e.target = e.srcElement ? e.srcElement : e.target;
				if (e.touches && e.touches.length > 0) {
					e.clientX = e.touches[0].clientX;
					e.clientY = e.touches[0].clientY;
					e.pageX = e.touches[0].pageX;
					e.pageY = e.touches[0].pageY;
					e.screenX = e.touches[0].screenX;
					e.screenY = e.touches[0].screenY;
				}
				if (!e.preventDefault) e.preventDefault = function(){ this.returnValue = false; }
				if (!e.stopPropagation) e.stopPropagation = function(){ this.cancelBubble = true; }
			} 
			return e;
		}
		
		__hjs.sup = function(t, c) {
			return function() { return c.apply(t, arguments); }
		}
		
		trace = function() {
			if (typeof console !== "undefined" && console.log) {
				if (console.log.apply) {
					console.log.apply(console,arguments);
				} else {
					console.log(arguments[0]);
				}
			}
		}
		
		__hjs.bind = function(s, f) {
			var n;
			if (f.bind) {
				n = f.bind(s);
			} else {
				n = function() { return arguments.callee.f.apply(arguments.callee.s,arguments); };
			}
			n.f = f;
			n.s = s;
			return n;
		}
	
		loadModule = function(c, f) {
			if (window.humboldtReadyHandlers == undefined) window.humboldtReadyHandlers = [];
			window.humboldtReadyHandlers.push(f);
	
			__hjs.include(c, true);
			__hjs.ready(null);
		}
		
		hasModule = function(c) {
			return (inc.indexOf(c) != -1);
		}
		
		dynamicModule = function(c) {
			return eval("new " + c + "();");
		}
		
		__hjs.castAs = function (o, c) {
			return __hjs.isOfType(o, c) ? o : null;
		}
		
		__hjs.isImplementation = function (o, c) {
			o = o.__implements;
			if (o) {
				for (var i = 0; i < o.length; i ++) {
					if (o[i] == c) return true;
				}
			}
			return false;
		}
		
		__hjs.isOfType = function(o,c) {
			if (o == null) return false;
			if (c == null) return true;
			if (c == Object) return (typeof o == "object");
			if (c == String) return (typeof o == "string");
			if (c == Boolean) return (typeof o == "boolean");
			if (typeof c == "object" && c["interface"]) return __hjs.isImplementation(o, c["interface"]);
			if (typeof c == "string") return o.nodeName==c.toUpperCase();
			return o instanceof c;
		}
	})();

}