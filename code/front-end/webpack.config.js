//https://webpack.js.org/configuration https://github.com/isel-leic-daw/2122v-public/blob/main/code/js/first/webpack.config.js
const HtmlWebpackPlugin = require('html-webpack-plugin')
module.exports = { 
    entry: "./src/index.tsx",
    output: {
        /* publicPath: '/' */
        filename: 'bundle.js',
    },
    mode: 'development',
    //dev tools is on by default when on development mode I think. On Chrome's Dev Tools at the bottom there should be a section called "front-end" w/ /node_modules/, /public/ and /src/. This will greatly improve debugging capabilities.
    //devtool: "eval", //https://webpack.js.org/configuration/devtool/ https://blog.teamtreehouse.com/introduction-source-maps
    devServer: { //the port is 8080 by default
        hot: false, //https://webpack.js.org/guides/hot-module-replacement/
        historyApiFallback: true, //establishes that paths that are not found directly, should be handled by the index.html (and it's javascript), assuming it uses HTML5 History API (which React Router uses). Per example, if this was false, an acess to a path other than'/' per example when in /about, it will say GET - Not found (which is a response from the webpack server). https://webpack.js.org/configuration/dev-server/#devserverhistoryapifallback
        proxy: { //alternative to overriding CORS in the server
            '/': 'http://localhost:9000' //should be the same port as used in the back-end
        } 
    },
    resolve: {
        extensions: ['.js', '.ts', '.jsx', '.tsx'],
    },
    module: {
        rules: [ //https://webpack.js.org/loaders/
            {
                test: /\.tsx?$/, //tells webpack what file types to look for
                use: 'ts-loader',
                exclude: /node_modules/,
            },
            { //https://webpack.js.org/loaders/css-loader/
                test: /\.css$/, 
                use: ["style-loader"]
            },

            { //https://webpack.js.org/loaders/css-loader/
                test: /\.css$/, 
                loader: "css-loader",
                options: { //https://stackoverflow.com/q/69905828/9375488   https://github.com/webpack-contrib/css-loader/issues/256#issuecomment-288460824     https://github.com/webpack-contrib/css-loader/issues/1380
                    url: true, //solves url() calls in the .css files in webpack 4.10.
                    esModule: false //aparently this disables file name hashing
                }
            },

            /* { //https://webpack.js.org/guides/asset-management/#loading-fonts //overrules previous rule's files, dont use
                test: /\.(png|ttf|.css)$/i,
                loader: 'url-loader',
                type: 'asset/resource',
                options: {
                    esModule: false
                }
            }, */

            { //https://stackoverflow.com/a/71519581/9375488
                //test: /\.mp3$/,
                test: /\.(png|svg|jpg|jpeg|gif|ogg|mp3|wav|ttf)$/i,
                loader: 'file-loader',
                options: { 
                    name: '[name].[ext]', //disables file name hashing
                    esModule: false, //Makes sure media files are correctly bundled https://stackoverflow.com/questions/59070216/webpack-file-loader-outputs-object-module I think it allows the use of 'require()' to import the files
                },
            },
            
         
        ],
    },

    //webpack gets by defalt all the files in /public/ that have convention names like index.html and favicon.ico, but it doesn't copy them to the dist folder. This plugin does that and you can indicate the names manually.
    plugins: [ //https://webpack.js.org/plugins/html-webpack-plugin/ https://stackoverflow.com/a/33519539/9375488
        new HtmlWebpackPlugin({ //the output file in /dist/ will always be index.html (if we dont specify w/ <filename>), no matter the name of the template
          template: 'public/index.html',
          favicon: 'public/favicon.ico' //Used just to copy the file to the dist folder, because on the build process of modifying the dist/index.html, it isn't including the / like /favico.ico, necessary for the icon to show up in the sub routes. So I manually added the line in the index.html document.
        })
    ]
}