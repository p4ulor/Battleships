# 1

- Origin = HTTP + localhost + 9000
- Origin == schema / host / Port

Site is less restrictive
- Site -> TLD+1. Top Level Domain

## different origin, same site
- http://www.example.com
- http://api.example.com

- example.co.uk ->  The eTLD = "co.uk". It has 2 segments
- example.co.uk = eTLD+1

## Same site: (eTLD+1)
- https://example.co.uk -> eTLD+1 = example.co.uk
- https://abc.xyz.example.co.uk -> eTLD+1 = example.co.uk

They have different origins, so they don't share cookies

- http://localhost:8080 -> localhost: Same Site. port:8080: Different origins
- http://localhost:8081 -> localhost: Same Site. port:8081: Different origins

Hostname defines sites

Port and domains defines origins

Different origins protect against cross fire scripting by not sharing cookies

Different origins, same site:
- example.to.uk
- example.co.uk

## About token storage
- Neither sessionStorage or localStorage will be used to store user tokens. Since that data is vulnerable to Cross Site Scripting
- Cookies will be used, which doesn't use javascript to access. The basic param the cookie receives as input is `HttpOnly`. The browser will automatically bind the cookie on HTTP requests. 

But the problem/hack called Cross Site Requet Forjery comes into play. But using the parameter `SameSite` in the cookie avoids this

Using cookies open up vulnerability: Cross Site Request Forgery

Cookie Atributes:
- HttpOnly //fixes javascript token access
- SameSite=Strict //fixes CSRF

# 5
React router navigate methods have a high priority, which makes it possible that the react router methods are executed first than settingState in useState vars


um componente define o seu estado pelo:
A components defines it's state by it's:
- props
- useStates

Web socket's é bidirectional: servidor-cliente
server side events: é uni direcional, como o HTTP é, mas usando message chunks, faz com q a comuniaçao "seja" bidirecional

Solve navigate()'s react router, precedence: 
- if(redirect) return <Navigate> <Home><Navigate/>

Setting states from useState's vars inside useEffect destructor returned function causes infinite loops!

If you have the error:
```js
react-dom.development.js:16227 Uncaught Error: Invalid hook call. Hooks can only be called inside of the body of a function component.
```
This could happen for one of the following reasons:
1. You might have mismatching versions of React and the renderer (such as React DOM)
2. You might be breaking the Rules of Hooks
3. You might have more than one copy of React in the same app
