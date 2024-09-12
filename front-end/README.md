# front-end
<p align="center">
    <img class="center" style="width: 350px; margin: auto auto;" src="../docs/imgs/cover_front-end.jpeg" />
</p>

This code module has the necessary code and media (both static-content) for a modern browser that uses Javascript, CSS, HTML, images and other media, in order for a client to interact with our API/Server with a friendly user interface

The intended output result will be 1 .html file, 1 .js file and other media files that need to be included in the `src/main/resources/public/` folder, which the API will use, so when a client tries to connect the server in the first time, the .html page is sent, along with the .js file that it references. This .js file will contain all of React's magic, which consists of modifying the HTML document tree to display whatever content we programmed to show-up.

## 1 - Install the dependencies in package.json (the resulting node_modules folder will be 100mb)
- npm install

## 2-a - Build src files and copy media files (to ./dist/) (allows access to source code in browser inspect/dev tools)
- npm run build

## 2-b - Build final/production (1 line .js) file and dependent files (to ./dist/)
- npm run prod

## 2-c Run a webpack mock-server to easily try it out in the browser
- npm run start
- Will be available at http://localhost:8080/

Running the Webpack mock-server is recommended during development because it auto-updates on code changes. Note: it doesn't build files. And it only listens to changes in src.

Note that this folder contains certain language syntaxes and libraries that can only be run with NodeJS ([see](https://github.com/isel-leic-daw/s2223i-51d-51n-public/blob/main/docs/lecture-notes/03.0-the-browser-application-platform.md)), BUT the end product must be pure (vanilla) Javascript in order for the browser to interpret. And `webpack` is the library that does it for us

# Dependencies included
### React
- [@types/react](https://www.npmjs.com/package/@types/react) -> Provides us with type definitions when calling React functions
- [@types/react-dom](https://www.npmjs.com/package/@types/react-dom) -> Provides us with type definitions when calling React functions
- [react](https://www.npmjs.com/package/react) -> Facilitates the creation of HTML pages, extending the syntax of JS (and is called JSX) that allows mixing between HTML and JS code. And some other things regarding UI. [see](https://github.com/isel-leic-daw/s2223i-51d-51n-public/blob/main/docs/lecture-notes/03.1.react.md)
- [react-dom](https://www.npmjs.com/package/react-dom) -> Provides DOM-specific methods to manipulate a HTML page
- [react-router-dom](https://www.npmjs.com/package/react-router-dom) -> Used for rendering or altering Components that are incorporated into the DOM when the page is navigated throughout it's use. [See](https://blog.webdevsimplified.com/2022-07/react-router/) and [this](https://www.youtube.com/watch?v=Ul3y1LXxzdU). This library is not officially from React, but it's great. It does it's thing by calling browser-DOM functions, per example: history. [docs](https://reactrouter.com/en/main)
#### Webpack
- [webpack-cli](https://www.npmjs.com/package/webpack-cli) -> For bundling AKA compressing the code. CLI -> Comand Line Interface, This version of webpack allows and expects a configuration file named `webpack.config.js` which allows for a more customizable build. Webpack compresses all of our Typescript or Javascript files into 1 .js file. This will result in reduced GET requests of files, reduce network load in the initial HTTP request of the client and prevents reverse engineering or hacking.
- [webpack-dev-server](https://www.npmjs.com/package/webpack-dev-server) -> A server that auto bundles, when you change code, meant to be used during development. For avoiding doing `npm webpack` and refreshing the browser
- [style-loader](https://www.npmjs.com/package/style-loader) -> Inject CSS into the DOM
- [css-loader](https://www.npmjs.com/package/css-loader) ->  interprets @import and url() like import/require() and will resolve them.
- [html-webpack-plugin](https://www.npmjs.com/package/html-webpack-plugin) -> Plugin that simplifies creation of HTML files to serve your bundles. [See](https://webpack.js.org/plugins/html-webpack-plugin/)
- [file-loader](https://www.npmjs.com/package//file-loader) -> For loading images and sound files. [See](https://v4.webpack.js.org/loaders/file-loader/)
- [ts-loader](https://www.npmjs.com/package/ts-loader) -> Allows the use of/integrates webpack with typescript, [See](https://webpack.js.org/guides/typescript/)
### Typescript
- [typescript](https://www.npmjs.com/package/typescript) -> An extension to javascript, whose main purpose is to add types to the language. Which helps in avoiding errors during development

## Directory content list
- `public` -> HTML, CSS and media files
- `src` -> React code using Typescript
- `package.json` -> Defines scripts and dependencies
- `package-lock.json` -> Ensures the integrity/compatibility of the NPM dependencies
- `webpack.config.js` -> Config for `webpack` to use, to produce the build
- `tsconfig.json` -> Configures how Typescript will be transformed to Javacript

## Notes
- In VSC upper controls, consider toggling: View -> Word Wrap (or ALT+Z) in certain ocassions. I prefer letting my code "breathe" or leaving comments as close as possible to the code related to it (usually in the same line), but further away from the side because it can be distracting or obfuscaste sometimes IMO
- Try deleting the .html to see what happens. Webpack creates a default .html page listing all file contents
- By default Webpack will try to use an .html file in `public/.index.html`. I installed the `html-webpack-plugin` because of [this](https://stackoverflow.com/questions/32155154/webpack-config-how-to-just-copy-the-index-html-to-the-dist-folder). See the last lines of [webpack.config.js](webpack.config.js)
- VSC tip: when naming `div`'s (and other things), you can type `div.<CSSclassName>` and when pressing enter it will create `<div className="<CSSclassName>">`
- React Router avoids reloading entire documents, contrary to using link ref \<a\>
- I tried to avoid using a CSS bootstrap because I would have to learn how to use the bootstrap and it would force me to add a lot of other elements and know what classes to assign to elements just to make a certain look work and could make the writing of the JSX a bit more confusing. And without it I have more control and learn more about CSS
- During development and testing using Chrome Dev Tools, some scripts in a VM-12345.MySrcFile.tsx will run and will look like clones of your code and run things your code is already running. What's the most likely explanation, for my case, is [this](https://stackoverflow.com/a/30321123/9375488) although the other answers can provide more info.

### About React
- https://reactjs.org/docs/dom-elements.html
- https://reactjs.org/docs/lifting-state-up.html
- https://create-react-app.dev/docs/adding-images-fonts-and-files/
- The naming and indication of CSS properties in react can be quite different! The values indicated can only be the strings. Only camel case properties are allowed. Per example, `border-radius` doesn't exist and there's no `class`, it's `className`. And the name suggestion/autocompletion can be weird sometimes. It's recommended to define all styles in a .css file.
- [react-component-self-close-on-button-click](https://stackoverflow.com/questions/52622578/react-component-self-close-on-button-click)
- [react-closing-a-dropdown-when-click-outside](https://stackoverflow.com/questions/63359138/react-closing-a-dropdown-when-click-outside)
### About CSS config
- [webpack-not-loading-css](https://stackoverflow.com/questions/34963051/webpack-not-loading-css)
- [with-webpack-why-should-one-import-css-files-from-js-source-code-and-not-build](https://stackoverflow.com/questions/47921082/with-webpack-why-should-one-import-css-files-from-js-source-code-and-not-build)
- [css-modules-webpack](https://blog.jakoblind.no/css-modules-webpack/)

### All media is copyright/royalty-free or was created/edited by me
