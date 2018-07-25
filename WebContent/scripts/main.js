/**
 * Note 1: all the codes are in a immediately-invoked function(IIF).
 * 		 why we use IIF? (1) we need to create a local scope to protect the stuff inside;
 * 							if you add another JavaScript file, the local variables or functions
 * 							won't affect each other.
 * 						 (2) we don't need to give it a name.
 * Note 2: 这个匿名函数修改了DOM tree. 通过JavaScript去修改html的内容。			
 */

(function () {

/**
 * Variables
 * Note: 你也可以这么声明变量:
 * 		 var user_id       = '',
 *           user_fullname = '',
 *           lng           = -122.08,
 *           lat           = 37.38;
 */
var user_id       = '';
var user_fullname = '';
var lng           = -122.08;
var lat           = 37.38;

/**
 * Initialize
 * note: init()相当于Java中的main函数，即入口函数
 */
function init() {
  // Register event listeners: 当你点击相应的button时，会运行相应的函数
  $('login-btn').addEventListener('click', login);
  // the following three are on the three buttons: Nearby, My Favorite, Recommendation.
  $('nearby-btn').addEventListener('click', loadNearbyRestaurants);
  $('fav-btn').addEventListener('click', loadFavoriteRestaurants);
  $('recommend-btn').addEventListener('click', loadRecommendedRestaurants);
  
  validateSession(); // 登陆验证
  
//  onSessionValid({ // 登陆成功后，传入一个对象，它包含用户的基本信息
//	  user_id: '1111',
//	  name: 'Joe Jiang'
//  });
}



/**
 * Session
 * note: 验证登陆
 */
function validateSession() {
  // The request parameters
  var url = './LoginServlet';
  var req = JSON.stringify({});
	  
  // display loading message
  showLoadingMessage('Validating your session...');

  // make AJAX call
  ajax('GET', url, req,
    // session is still valid
    function (res) {
	  var result = JSON.parse(res);

	  if (result.status === 'OK') {
		onSessionValid(result);
	  }
    }
  );
}

/**
 * onSessionValid(result) 在登陆成功后，根据传入的用户信息，在页面上显示该显示的内容，隐藏该隐藏的。
 * @param result：用户信息
 * @returns
 */

function onSessionValid(result) {
  user_id = result.user_id;
  user_fullname = result.name;
	  
  var loginForm = $('login-form');
  var restaurantNav = $('restaurant-nav');
  var restaurantList = $('restaurant-list');
  var avatar = $('avatar');
  var welcomeMsg = $('welcome-msg');
  var logoutBtn = $('logout-link');
  
  welcomeMsg.innerHTML = 'Hello, ' + user_fullname;
  
  // 显示出相应的元素于页面
  showElement(restaurantNav);
  showElement(restaurantList);
  showElement(avatar);
  showElement(welcomeMsg);
  showElement(logoutBtn, 'inline-block');
  // 不显示一些元素
  hideElement(loginForm);

  initGeoLocation();
}

// onSessionInvalid() 在登陆失败后，在页面上显示该显示的内容，隐藏该隐藏的。
function onSessionInvalid() {
  var loginForm = $('login-form');
  var restaurantNav = $('restaurant-nav');
  var restaurantList = $('restaurant-list');
  var avatar = $('avatar');
  var welcomeMsg = $('welcome-msg');
  var logoutBtn = $('logout-link');

  //隐藏一些元素
  hideElement(restaurantNav);
  hideElement(restaurantList);
  hideElement(avatar);
  hideElement(logoutBtn);
  hideElement(welcomeMsg);
  //显示一些元素
  showElement(loginForm);
}

// 显示当前位置
function initGeoLocation() {
  if (navigator.geolocation) { // note: navigator是window里的对象, 它包含很多信息, 其中包括geolocation这个对象
	// note: geolocation的__proto__指向的共有空间中包含getCurrentPosition()函数，它有三个参数:
	//       当成功获取经纬度信息后，执行第一个callback function, 即onPositionUpdated;
	//       当没法获取经纬度信息后，执行第二个callback function, 即onLoadPositionFailed;  
	//       第三个参数设定缓存信息多少秒，因为跟卫星拿信息耗时间，缓存下来，下次run起来就快一些。  
    navigator.geolocation.getCurrentPosition(onPositionUpdated, onLoadPositionFailed, {maximumAge: 60000});
	showLoadingMessage('Fetching your location...');
  } else {
    onLoadPositionFailed();
  }
}

function onPositionUpdated(position) {
  lat = position.coords.latitude;
  lng = position.coords.longitude;

  loadNearbyRestaurants(); // 跟后端进行通信！！！
}

function onLoadPositionFailed() {
  console.warn('navigator.geolocation is not available');
  //loadNearbyRestaurants();
  getLocationFromIP();
}

function getLocationFromIP() {
  // Get location from http://ipinfo.io/json
  var url = 'http://ipinfo.io/json'
  var req = null;
  ajax('GET', url, req,
    // session is still valid
    function (res) {
      var result = JSON.parse(res);
      if ('loc' in result) {
        var loc = result.loc.split(',');
        lat = loc[0];
        lng = loc[1];
      } else {
        console.warn('Getting location by IP failed.');
      }
      loadNearbyRestaurants();
    }
  );
}

//-----------------------------------
//  Login
//-----------------------------------

function login() {
  var username = $('username').value;
  var password = $('password').value;
  password = md5(username + md5(password)); // in our database, we've stored the hashed value instead of the original password for the concern of security.
  
  // The request parameters
  var url = './LoginServlet';
  var params = 'user_id=' + username + '&password=' + password;
  var req = JSON.stringify({});

  ajax('POST', url + '?' + params, req,
    // successful callback
    function (res) {
      var result = JSON.parse(res);
      
      // successfully logged in
      if (result.status === 'OK') {
    	onSessionValid(result);
      }
    },
    // error
    function () {
      showLoginError();
    }
  );
}

function showLoginError() {
    $('login-error').innerHTML = 'Invalid username or password';
}

function clearLoginError() {
	$('login-error').innerHTML = '';
}

// -----------------------------------
//  Helper Functions
// -----------------------------------

/**
 * activeBtn(btnId)
 * A helper function that makes a navigation button active
 * 
 * @param btnId - The id of the navigation button
 */
function activeBtn(btnId) {
  var btns = document.getElementsByClassName('main-nav-btn'); // all three buttons have the class name 'main-nav-btn'

  // deactivate all navigation buttons
  for (var i = 0; i < btns.length; i++) { // iterate over each button, i.e., each anchor
    btns[i].className = btns[i].className.replace(/\bactive\b/, ''); // regular expression：\b表示边界，包括空格，回车，换行, etc; \s表示空格
  }
  
  // active the one that has id = btnId
  var btn = $(btnId);
  btn.className += ' active'; // 在className中添加active字样就激活了这个button
}

function showLoadingMessage(msg) {
  var restaurantList = $('restaurant-list');
  restaurantList.innerHTML = '<p class="notice"><i class="fa fa-spinner fa-spin"></i> ' + msg + '</p>';
}

function showWarningMessage(msg) {
  var restaurantList = $('restaurant-list');
  restaurantList.innerHTML = '<p class="notice"><i class="fa fa-exclamation-triangle"></i> ' + msg + '</p>';
}

function showErrorMessage(msg) {
  var restaurantList = $('restaurant-list');
  restaurantList.innerHTML = '<p class="notice"><i class="fa fa-exclamation-circle"></i> ' + msg + '</p>';
}

/**
 * $(tag, options) is a helper function that
 * (1) when options is not given, get the element corresponding to the given tag(i.e., id);
 * (2) when options is given, creates a DOM element <tag options...>
 * note: 1. for a not-given parameter, its value is undefined.
 * 		 2. 这里$()和jQuery无关，只是自定义的函数, 自己封装的.
 * 		
 * @param tag
 * @param options
 * @returns
 */
function $(tag, options) {
  if (!options) { // note: if options is false/null/undefined, this if statement will be executed.
    return document.getElementById(tag); // tag is an id
  }

  var element = document.createElement(tag);

  for (var option in options) {
    if (options.hasOwnProperty(option)) {
      element[option] = options[option]; // 对属性赋值:将options的内容传递给新创建的element
    }
  }

  return element;
}

function hideElement(element) {
  element.style.display = 'none'; // 页面上就不显示此元素了
}

function showElement(element, style) {
  var displayStyle = style ? style : 'block'; // 若style是undefined, assign 'block' to displayStyle; 若style给了值，assign style to displayStyle. 
  element.style.display = displayStyle;
}

/**
 * AJAX helper
 * 
 * @param method - GET|POST|PUT|DELETE
 * @param url - API end point
 * @param callback - This the successful callback
 * @param errorHandler - This is the failed callback
 */
function ajax(method, url, data, callback, errorHandler) {
  var xhr = new XMLHttpRequest(); // XMLHttpRequest处理异步通信

  xhr.open(method, url, true);

  xhr.onload = function () {
	switch (xhr.status) {
	  case 200:
		callback(xhr.responseText);
		break;
	  case 403:
		onSessionInvalid();
		break;
	  case 401:
		errorHandler();
		break;
	}
  };

  xhr.onerror = function () {
    console.error("The request couldn't be completed.");
    errorHandler();
  };

  if (data === null) {
    xhr.send();
  } else {
    xhr.setRequestHeader("Content-Type", "application/json;charset=utf-8");
    xhr.send(data);
  }
}

// -------------------------------------
//  AJAX call server-side APIs
//  Note: API #1, #2, #3大同小异, 对应网页上三个button:NearBy, My Favorites, Recommendation; 都是GET数据，再在html上添加出来；都要用到一个核心的辅助函数listRestaurants()
//		  API #4是POST/DELETE数据, 对应的是网页上添加或者删除favorite餐馆
// -------------------------------------

/**
 * API #1
 * note: 这个API对应的是网页上NearBy button.
 * Load the nearby restaurants
 * API end point: [GET] /Dashi/restaurants?user_id=1111&lat=37.38&lon=-122.08
 * 
 */
function loadNearbyRestaurants() {
  console.log('loadNearbyRestaurants');
  activeBtn('nearby-btn'); // 让此button处于active状态

  // The request parameters
  var url = './restaurants'; // ./表示用当前路径
  var params = 'user_id=' + user_id + '&lat=' + lat + '&lon=' + lng;
  var req = JSON.stringify({}); // 因为这里用的是GET, 而不是POST，所以在http的body里用了一个空的JSON object. Note: JSON.stringify()将object变成string
  
  // display loading message
  showLoadingMessage('Searching for restaurants nearby...');
  
  // make AJAX call
  ajax('GET', url + '?' + params, req, 
    // successful callback
    function (res) {// res(server的response)是string data
      var restaurants = JSON.parse(res); // 将res解析成array of JSON objects
      if (!restaurants || restaurants.length === 0) {
        showWarningMessage('No nearby restaurant.');
      } else {
        listRestaurants(restaurants);
      }
    },
    // failed callback
    function () {
      showErrorMessage('Cannot load nearby restaurants.');
    }  
  );
}

/**
 * API #2
 * note: 这个API对应的是网页上My Favorites button.
 * Load favorite (or visited) restaurants
 * API end point: [GET] /Dashi/history?user_id=1111
 */
function loadFavoriteRestaurants(event) {
  event.preventDefault();
  activeBtn('fav-btn');

  // The request parameters
  var url = './history';
  var params = 'user_id=' + user_id;
  var req = JSON.stringify({});
  
  // display loading message
  showLoadingMessage('Loading favorite restaurants...');

  // make AJAX call
  ajax('GET', url + '?' + params, req, 
    function (res) {
      var restaurants = JSON.parse(res);
      if (!restaurants || restaurants.length === 0) {
        showWarningMessage('No favorite restaurant.');
      } else {
        listRestaurants(restaurants);
      }
    },
    function () {
      showErrorMessage('Cannot load favorite restaurants.');
    }  
  );
}

/**
 * API #3
 * note: 这个API对应的是网页上Recommendation button.
 * Load recommended restaurants
 * API end point: [GET] /Dashi/recommendation?user_id=1111
 */
function loadRecommendedRestaurants() {
  activeBtn('recommend-btn');

  // The request parameters
  var url = './recommendation';
  var params = 'user_id=' + user_id;
  var req = JSON.stringify({});
  
  // display loading message
  showLoadingMessage('Loading recommended restaurants...');

  // make AJAX call
  ajax('GET', url + '?' + params, req,
    // successful callback
    function (res) {
      var restaurants = JSON.parse(res);
      if (!restaurants || restaurants.length === 0) {
        showWarningMessage('No recommended restaurant. Make sure you have favorites.');
      } else {
        listRestaurants(restaurants);
      }
    },
    // failed callback
    function () {
      showErrorMessage('Cannot load recommended restaurants.');
    } 
  );
}

/**
 * API #4
 * Toggle favorite (or visited) restaurants
 * 
 * @param business_id - The restaurant business id
 * 
 * API end point: [POST]/[DELETE] /Dashi/history
 * request json data: { user_id: 1111, visited: [a_list_of_business_ids] }
 */
function changeFavoriteRestaurant(business_id) {
  // Check whether this restaurant has been visited or not
  var li = $('restaurant-' + business_id); // 找到餐馆id对应的li element
  var favIcon = $('fav-icon-' + business_id); 
  var isVisited = li.dataset.visited !== 'true';  // 取反
  
  // The request parameters
  var url = './history';
  // 将包含信息的object变成string
  var req = JSON.stringify({ 
    user_id: user_id,
    visited: [business_id]
  });
  var method = isVisited ? 'POST' : 'DELETE'; 

  ajax(method, url, req,
    // successful callback
    function (res) {
      var result = JSON.parse(res);
      if (result.status === 'OK') {
        li.dataset.visited = isVisited;
        favIcon.className = isVisited ? 'fa fa-heart' : 'fa fa-heart-o'; // note: 'fa fa-heart'是实心心形；'fa fa-heart-o'是空心心形
      }
    }
  );
}

// -------------------------------------
//  Create restaurant list
// -------------------------------------

/**
 * List restaurants
 * note: 这个function是用于API #1，#2, #3里的，辅助列出餐馆列表的函数。
 * 
 * @param restaurants - An array of restaurant JSON objects
 */
function listRestaurants(restaurants) {
  // Clear the current results
  var restaurantList = $('restaurant-list');
  restaurantList.innerHTML = '';

  for (var i = 0; i < restaurants.length; i++) {
    addRestaurant(restaurantList, restaurants[i]); // add each restaurant to the list; each restaurants[i] is a li in ul
  }
}

/**
 * Add restaurant to the list on the web page
 * 
 * @param restaurantList - The <ul id="restaurant-list"> tag
 * @param restaurant - The restaurant data (JSON object)
 */
function addRestaurant(restaurantList, restaurant) {
  var business_id = restaurant.business_id;
  
  // create the <li> tag and specify the id and class attributes。Note: id and class信息都被添加到html的tag里，即<li >里。 
  var li = $('li', {
    id: 'restaurant-' + business_id,
    className: 'restaurant' // 这里的className对应的是html里的class
  });
  
  // set the data attribute. Note: 这些信息都被添加到html的tag里，即<li >里。 这个dataset是html5的新的特性, 可以通过它去保存一些有用的信息，用自定义的attribute.
  li.dataset.business = business_id;
  li.dataset.visited = restaurant.is_visited;

  // restaurant image. Note: $('img', {src: restaurant.image_url})创建了image tag; 将其作为一个子节点放入li中下一层级
  li.appendChild($('img', {src: restaurant.image_url}));

  // section, 其实是div   note: create a div element
  var section = $('div', {});
  
  // title
  var title = $('a', {href: restaurant.url, target: '_blank', className: 'restaurant-name'}); // target是要跳转到的页面；
  title.innerHTML = restaurant.name;
  section.appendChild(title); 
  
  // category
  var category = $('p', {className: 'restaurant-category'});
  category.innerHTML = 'Category: ' + restaurant.categories.join(', ');// restaurant.categories is an array;.join()将array变成string
  section.appendChild(category);
  
  // stars
  var stars = $('div', {className: 'stars'});
  for (var i = 0; i < parseInt(restaurant.stars); i++) { // 根据star的个数，添加相应那么多个心形元素. Note: restaurant.stars默认情况下是浮点数，而非整数, 所以用parseInt()取整
    var star = $('i', {className: 'fas fa-star'});
    stars.appendChild(star);
  }

  if (('' + restaurant.stars).match(/\.5$/)) { // 若出现，比如，4.5的评分时，我们添加半颗心的图形。Note：这里用了regular expression
    stars.appendChild($('i', {className: 'fas fa-star-half-alt'}));
  }

  section.appendChild(stars); 

  li.appendChild(section);

  // address
  var address = $('p', {className: 'restaurant-address'});
  
  address.innerHTML = restaurant.full_address.replace(/,/g, '<br/>'); // <br/>是html里的换行
  li.appendChild(address);

  // favorite link
  var favLink = $('div', {className: 'fav-link'});
  
  // note: both .addEventListener and .onclick can register event listener
  favLink.onclick = function () {
    changeFavoriteRestaurant(business_id); // note: 当我点击link时，执行这个function
  };
  
  favLink.appendChild($('i', {
    id: 'fav-icon-' + business_id,
    className: restaurant.is_visited ? 'fas fa-heart' : 'far fa-heart'
  }));
  
  li.appendChild(favLink);

  restaurantList.appendChild(li);
}

init(); // run入口函数

})();