/**
 * TravelBuddy - Web Application Logic
 * Orchestrates splash transitions and user interactions.
 */

function initTravelBuddyApp() {
  const splashScreen = document.getElementById('splash-screen');
  const mainApp = document.getElementById('main-app');
  const loginForm = document.getElementById('login-form');
  const signupForm = document.getElementById('signup-form');
  const loginSection = document.getElementById('login-section');
  const signupSection = document.getElementById('signup-section');

  // Toggle between Login and Signup
  const signupLinkId = document.getElementById('signup-link-id');
  const signinLinkId = document.getElementById('signin-link-id');

  if (signupLinkId) {
    signupLinkId.addEventListener('click', (e) => {
      e.preventDefault();
      if(loginSection) {
        loginSection.classList.remove('visible');
        loginSection.classList.add('hidden');
      }
      if(signupSection) {
        signupSection.classList.remove('hidden');
        signupSection.classList.add('visible');
      }
    });
  }

  if (signinLinkId) {
    signinLinkId.addEventListener('click', (e) => {
      e.preventDefault();
      if(signupSection) {
        signupSection.classList.remove('visible');
        signupSection.classList.add('hidden');
      }
      if(loginSection) {
        loginSection.classList.remove('hidden');
        loginSection.classList.add('visible');
      }
    });
  }

  // 1. Splash Screen Transition Pipeline
  // Displays splash screen for 2500ms, then fades it out and shows main app.
  const splashTimeout = setTimeout(() => {
    // Start the fade out transition
    splashScreen.classList.add('fade-out');

    // Wait for the CSS transition (800ms) to complete, then make the app visible
    setTimeout(() => {
      splashScreen.style.display = 'none'; // Clear from rendering tree
      mainApp.classList.add('visible');
    }, 800);
  }, 2500);

  // 2. Interactive Form Submission & Dashboard Transition
  if (loginForm) {
    loginForm.addEventListener('submit', (e) => {
      e.preventDefault();
      
      const email = document.getElementById('email').value.trim();
      const password = document.getElementById('password').value;

      if (!email || !password) {
        alert('Please fill in both email and password fields.');
        return;
      }

      // Add a cool, premium loading indicator state to the button
      const submitBtn = loginForm.querySelector('.btn-submit');
      const originalText = submitBtn.innerHTML;
      submitBtn.disabled = true;
      submitBtn.innerHTML = `
        <svg class="spinner" viewBox="0 0 50 50" style="width: 24px; height: 24px; animation: rotate 2s linear infinite; margin-right: 8px;">
          <circle class="path" cx="25" cy="25" r="20" fill="none" stroke="currentColor" stroke-width="5" stroke-linecap="round" style="stroke-dasharray: 1, 150; stroke-dashoffset: 0; animation: dash 1.5s ease-in-out infinite;"></circle>
        </svg>
        Signing in...
      `;

      // Make active POST request to XAMPP backend login.php
      const formData = new FormData();
      formData.append('email', email);
      formData.append('password', password);

      fetch(`${BASE_URL}/login.php`, {
        method: 'POST',
        body: formData
      })
      .then(res => res.json())
      .then(data => {
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalText;

        if (data.status === 'success') {
          // Store user information dynamically inside LocalStorage for this session
          localStorage.setItem('ACTIVE_USER_NAME', data.full_name || 'simats');
          localStorage.setItem('ACTIVE_EMAIL', email);

          // Hide Login Panel
          const loginSection = document.getElementById('login-section');
          if (loginSection) {
            loginSection.classList.remove('visible');
            loginSection.classList.add('hidden');
          }
          
          // Show Dashboard Panel
          const dashboardSection = document.getElementById('dashboard-section');
          if (dashboardSection) {
            dashboardSection.classList.remove('hidden');
            dashboardSection.classList.add('visible');
          }
          
          // Initialize Geolocation & Dashboard Integration
          initializeDashboard();
        } else {
          showToast(data.message || 'Invalid email or password.');
        }
      })
      .catch(err => {
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalText;
        showToast('Server is experiencing high traffic right now, please try again.');
      });
    });
  }

  if (signupForm) {
    signupForm.addEventListener('submit', (e) => {
      e.preventDefault();
      
      const fullName = document.getElementById('signup-fullname').value.trim();
      const email = document.getElementById('signup-email').value.trim();
      const password = document.getElementById('signup-password').value;

      if (!fullName || !email || !password) {
        showToast('Please fill in all fields.');
        return;
      }

      const submitBtn = signupForm.querySelector('.btn-submit');
      const originalText = submitBtn.innerHTML;
      submitBtn.disabled = true;
      submitBtn.innerHTML = `
        <svg class="spinner" viewBox="0 0 50 50" style="width: 24px; height: 24px; animation: rotate 2s linear infinite; margin-right: 8px;">
          <circle class="path" cx="25" cy="25" r="20" fill="none" stroke="currentColor" stroke-width="5" stroke-linecap="round" style="stroke-dasharray: 1, 150; stroke-dashoffset: 0; animation: dash 1.5s ease-in-out infinite;"></circle>
        </svg>
        Creating Account...
      `;

      const formData = new FormData();
      formData.append('full_name', fullName);
      formData.append('email', email);
      formData.append('password', password);

      fetch('http://localhost/TravelBuddybackend/register.php', {
        method: 'POST',
        body: formData
      })
      .then(res => res.json())
      .then(data => {
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalText;

        if (data.status === 'success') {
          showToast(data.message || 'Account created successfully!');
          signupForm.reset();
          // Switch back to login screen
          signupSection.classList.remove('visible');
          signupSection.classList.add('hidden');
          loginSection.classList.remove('hidden');
          loginSection.classList.add('visible');
        } else {
          showToast(data.message || 'Failed to create account.');
        }
      })
      .catch(err => {
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalText;
        showToast('Server is experiencing high traffic right now, please try again.');
      });
    });
  }

  // ==========================================================================
  // 3. Dashboard Core Integration Logic
  // ==========================================================================

  const BASE_URL = 'http://localhost/TravelBuddybackend';

  function initializeDashboard() {
    // Attempt to track user's real live location via browser Geolocation API
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const lat = position.coords.latitude;
          const lng = position.coords.longitude;
          
          // Reverse-geocode coordinates using OpenStreetMap free Nominatim API
          const nominatimUrl = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}`;
          
          fetch(nominatimUrl)
            .then(res => res.json())
            .then(data => {
              const address = data.address || {};
              const city = address.city || address.town || address.village || address.municipality || 'Nellore';
              const country = address.country || 'India';
              
              // Load Map, Weather, and Labels
              loadLocationData(city, country, lat, lng);
            })
            .catch(() => {
              // Fallback to Nellore, India on geocode failure
              loadLocationData('Nellore', 'India', 14.4426, 79.9865);
            });
        },
        () => {
          // Fallback to Nellore, India if permission is denied or coordinates fail
          loadLocationData('Nellore', 'India', 14.4426, 79.9865);
        },
        { timeout: 10000, enableHighAccuracy: true }
      );
    } else {
      // Browser doesn't support Geolocation - Fallback to default
      loadLocationData('Nellore', 'India', 14.4426, 79.9865);
    }

    setupDashboardEvents();
  }

  // Updates layout markers and maps, and triggers backend Weather fetch
  function loadLocationData(city, country, lat, lng) {
    // 1. Update Labels
    document.getElementById('tvUpcomingTitle').textContent = city;
    document.getElementById('tvUpcomingDates').textContent = `Current: ${country}`;
    document.getElementById('tvWeatherLocation').textContent = `Weather in ${city}`;

    // 2. Reload Google Maps Embed focused on coordinates
    const mapIframe = document.getElementById('live-map-iframe');
    if (mapIframe) {
      mapIframe.src = `https://maps.google.com/maps?q=${lat},${lng}&t=&z=14&ie=UTF8&iwloc=&output=embed`;
    }

    // 3. Query weather.php backend for this specific place
    fetchWeather(city);
  }

  // ==========================================================================
  // 4. AJAX Weather API Integration
  // ==========================================================================
  function fetchWeather(place) {
    const weatherUrl = `${BASE_URL}/weather.php?place=${encodeURIComponent(place)}`;

    fetch(weatherUrl)
      .then(res => res.json())
      .then(data => {
        if (data.current_temp) {
          // Bind basic values
          const temp = data.current_temp.endsWith('°') ? `${data.current_temp}C` : data.current_temp;
          document.getElementById('weather-temp').textContent = temp;
          document.getElementById('weather-condition').textContent = data.condition;

          // Render Hourly forecast scroll grid
          const hourlyGrid = document.getElementById('hourly-forecast-id');
          hourlyGrid.innerHTML = '';
          if (data.hourly && data.hourly.length > 0) {
            data.hourly.forEach(hour => {
              const hourDiv = document.createElement('div');
              hourDiv.className = 'hourly-item';
              hourDiv.innerHTML = `
                <span class="hourly-time">${hour.time}</span>
                <span class="hourly-icon">${getWeatherIcon(hour.condition)}</span>
                <span class="hourly-temp">${hour.temp}</span>
              `;
              hourlyGrid.appendChild(hourDiv);
            });
          }

          // Render 7-day forecast scroll list
          const dailyList = document.getElementById('daily-forecast-id');
          dailyList.innerHTML = '';
          if (data.daily && data.daily.length > 0) {
            data.daily.forEach(day => {
              const dayDiv = document.createElement('div');
              dayDiv.className = 'daily-item';
              dayDiv.innerHTML = `
                <span class="daily-day">${day.day}</span>
                <span class="daily-condition">${day.condition} ${getWeatherIcon(day.condition)}</span>
                <span class="daily-temps">
                  <span class="daily-high">${day.high}</span>
                  <span class="daily-low">${day.low}</span>
                </span>
              `;
              dailyList.appendChild(dayDiv);
            });
          }
        }
      })
      .catch(err => {
        console.error('Weather backend fetch failed:', err);
      });
  }

  // ==========================================================================
  // 5. Ask Travel Buddy AI Chat Drawer Integration
  // ==========================================================================
  function setupDashboardEvents() {
    const weatherCard = document.getElementById('cvWeatherCard');
    const weatherDetails = document.getElementById('weather-details-id');
    const weatherToggle = document.getElementById('btn-weather-toggle-id');

    // Collapsible Weather forecast details panel toggle
    const toggleForecast = (e) => {
      e.stopPropagation();
      const isCollapsed = weatherDetails.classList.contains('collapsed');
      
      if (isCollapsed) {
        weatherDetails.classList.remove('collapsed');
        weatherToggle.classList.add('expanded');
        weatherToggle.setAttribute('aria-expanded', 'true');
      } else {
        weatherDetails.classList.add('collapsed');
        weatherToggle.classList.remove('expanded');
        weatherToggle.setAttribute('aria-expanded', 'false');
      }
    };
    
    weatherCard.addEventListener('click', toggleForecast);
    weatherToggle.addEventListener('click', toggleForecast);

    // Click Map Card to open full Google Maps view in new tab
    const mapCard = document.getElementById('cvUpcomingTrip');
    mapCard.addEventListener('click', () => {
      const city = document.getElementById('tvUpcomingTitle').textContent;
      window.open(`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(city)}`, '_blank');
    });

    // slide-up Chat Drawer bindings
    const askBuddyCard = document.getElementById('cvAskBuddy');
    const chatDrawer = document.getElementById('chat-drawer-id');
    const chatOverlay = document.getElementById('chat-overlay-id');
    const chatCloseBtn = document.getElementById('btn-chat-close-id');
    const chatForm = document.getElementById('chat-input-form');
    const chatInput = document.getElementById('chat-input-field');
    const chatMessages = document.getElementById('chat-messages-id');

    // Open Chat Drawer
    askBuddyCard.addEventListener('click', () => {
      chatDrawer.classList.add('open');
      chatOverlay.classList.add('visible');
      chatInput.focus();
    });

    // Close Chat Drawer
    const closeDrawer = () => {
      chatDrawer.classList.remove('open');
      chatOverlay.classList.remove('visible');
    };
    chatCloseBtn.addEventListener('click', closeDrawer);
    chatOverlay.addEventListener('click', closeDrawer);

    // Send chat question via POST AJAX
    chatForm.addEventListener('submit', (e) => {
      e.preventDefault();
      
      const question = chatInput.value.trim();
      if (!question) return;

      // 1. Add User bubble
      appendMessage(question, 'user');
      chatInput.value = '';
      
      // 2. Add dynamic typing indicator
      const typingId = 'typing-' + Date.now();
      const typingIndicator = document.createElement('div');
      typingIndicator.className = 'message ai typing-bubble';
      typingIndicator.id = typingId;
      typingIndicator.innerHTML = `
        <span class="dot-pulse"></span>
        <span class="dot-pulse"></span>
        <span class="dot-pulse"></span>
      `;
      chatMessages.appendChild(typingIndicator);
      chatMessages.scrollTop = chatMessages.scrollHeight;

      // 3. POST request to ask_buddy.php
      fetch(`${BASE_URL}/ask_buddy.php`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question: question })
      })
        .then(res => res.json())
        .then(data => {
          // Remove typing indicator bubble
          const indicator = document.getElementById(typingId);
          if (indicator) indicator.remove();

          if (data.status === 'success' && data.answer) {
            appendMessage(data.answer, 'ai');
          } else {
            appendMessage("I couldn't contact my tour brain. Let's try again in a bit!", 'ai');
          }
        })
        .catch(() => {
          const indicator = document.getElementById(typingId);
          if (indicator) indicator.remove();
          appendMessage("Oops! I had trouble reaching the AI server. Is the local backend running?", 'ai');
        });
    });

    function appendMessage(text, sender) {
      const bubble = document.createElement('div');
      bubble.className = `message ${sender}`;
      
      // Parse basic bold markers (**text**) into <strong> tags for a neat, rich-text look
      const formattedText = text
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\n/g, '<br>');
        
      bubble.innerHTML = `<p>${formattedText}</p>`;
      chatMessages.appendChild(bubble);
      
      // Keep scroll locked to bottom
      chatMessages.scrollTop = chatMessages.scrollHeight;
    }
  }

  // Helper mapping conditions to graphic emojis for gorgeous aesthetics
  function getWeatherIcon(condition) {
    const cond = condition.toLowerCase();
    if (cond.includes('sunny') || cond.includes('clear')) return '☀️';
    if (cond.includes('cloud') || cond.includes('overcast')) return '⛅';
    if (cond.includes('rain') || cond.includes('drizzle')) return '🌧️';
    if (cond.includes('snow') || cond.includes('hail')) return '❄️';
    if (cond.includes('thunder') || cond.includes('storm')) return '⛈️';
    if (cond.includes('fog') || cond.includes('mist') || cond.includes('haze')) return '🌫️';
    return '🌡️';
  }

  /* ==========================================================================
     Trip Buddies (Community Feed & Requests) Logic
     ========================================================================== */
  const communitySection = document.getElementById('community-section');
  const dashboardSection = document.getElementById('dashboard-section');
  
  // Navigation: Dashboard to Community
  const cvCommunityBtn = document.getElementById('cvCommunity');
  if (cvCommunityBtn) {
    cvCommunityBtn.addEventListener('click', () => {
      dashboardSection.classList.remove('visible');
      dashboardSection.classList.add('hidden');
      communitySection.classList.remove('hidden');
      communitySection.classList.add('visible');
      loadCommunityFeed();
    });
  }

  // Navigation: Back to Dashboard
  const btnCommBack = document.getElementById('btn-comm-back-id');
  if (btnCommBack) {
    btnCommBack.addEventListener('click', () => {
      communitySection.classList.remove('visible');
      communitySection.classList.add('hidden');
      dashboardSection.classList.remove('hidden');
      dashboardSection.classList.add('visible');
    });
  }

  // Switcher Tabs Logic
  const tabExplore = document.getElementById('tab-explore');
  const tabRequests = document.getElementById('tab-requests');
  const exploreFeedView = document.getElementById('explore-feed-view');
  const joinRequestsView = document.getElementById('join-requests-view');
  const commSearchBar = document.getElementById('comm-search-bar-id');

  if (tabExplore && tabRequests) {
    tabExplore.addEventListener('click', () => {
      tabExplore.classList.add('active');
      tabRequests.classList.remove('active');
      exploreFeedView.classList.remove('hidden');
      exploreFeedView.classList.add('visible');
      joinRequestsView.classList.remove('visible');
      joinRequestsView.classList.add('hidden');
      commSearchBar.style.display = 'flex';
      loadCommunityFeed();
    });

    tabRequests.addEventListener('click', () => {
      tabRequests.classList.add('active');
      tabExplore.classList.remove('active');
      joinRequestsView.classList.remove('hidden');
      joinRequestsView.classList.add('visible');
      exploreFeedView.classList.remove('visible');
      exploreFeedView.classList.add('hidden');
      commSearchBar.style.display = 'none';
      loadJoinRequests();
    });
  }

  // Create Post Modal Controls
  const createPostModal = document.getElementById('create-post-modal');
  const fabCreatePost = document.getElementById('fabCreatePost');
  const btnModalClose = document.getElementById('btn-modal-close-id');

  if (fabCreatePost && createPostModal) {
    fabCreatePost.addEventListener('click', () => {
      createPostModal.classList.remove('hidden');
      createPostModal.classList.add('visible');
    });
  }

  if (btnModalClose && createPostModal) {
    btnModalClose.addEventListener('click', () => {
      createPostModal.classList.remove('visible');
      createPostModal.classList.add('hidden');
    });
  }

  // Toast Alerts Helper matching Android Toasts
  function showToast(message) {
    let toast = document.getElementById('toast-container-id');
    if (!toast) {
      toast = document.createElement('div');
      toast.id = 'toast-container-id';
      toast.className = 'toast-alert';
      document.body.appendChild(toast);
    }
    toast.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg> <span>${message}</span>`;
    toast.classList.add('show');
    
    setTimeout(() => {
      toast.classList.remove('show');
    }, 2800);
  }

  // Load Explore Feed Posts
  const communityPostsList = document.getElementById('community-posts-list');
  const commSearchInput = document.getElementById('comm-search-input');
  let loadedPosts = [];

  function loadCommunityFeed() {
    communityPostsList.innerHTML = `<div class="empty-feed-state"><div class="dot-pulse"></div><div class="dot-pulse"></div><div class="dot-pulse"></div></div>`;
    
    fetch('http://localhost/TravelBuddybackend/community.php?action=get')
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success' && data.data) {
          loadedPosts = data.data;
          renderPostsFeed(loadedPosts);
        } else {
          communityPostsList.innerHTML = `<div class="empty-feed-state"><p>Could not load explore feed posts.</p></div>`;
        }
      })
      .catch(() => {
        communityPostsList.innerHTML = `<div class="empty-feed-state"><p>Error connecting to community backend.</p></div>`;
      });
  }

  function renderPostsFeed(posts) {
    if (posts.length === 0) {
      communityPostsList.innerHTML = `
        <div class="empty-feed-state">
          <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
          <p>No active travel plans found.<br>Be the first to share one!</p>
        </div>`;
      return;
    }

    communityPostsList.innerHTML = '';
    const activeUserName = localStorage.getItem('ACTIVE_USER_NAME') || 'Aarav Mehta';
    
    posts.forEach(post => {
      // Setup avatar color classes dynamically based on avatar_id
      const avatarId = post.avatar_id || 1;
      const avatarBg = `avatar-bg-${avatarId}`;
      const initials = post.user_name ? post.user_name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase() : 'TR';
      
      const isOwner = (post.user_name === activeUserName);
      const isRequested = localStorage.getItem(`requested_post_${post.id}`) === 'true';

      const card = document.createElement('div');
      card.className = 'feed-card';
      card.setAttribute('data-id', post.id);

      card.innerHTML = `
        <div class="card-profile">
          <div class="profile-avatar ${avatarBg}">${initials}</div>
          <div class="profile-info">
            <span class="profile-name">${post.user_name}</span>
            <span class="profile-rating">${post.user_rating || '4.8 Traveler Rating'}</span>
          </div>
          ${isOwner ? `
            <div class="profile-actions">
              <button class="btn-card-action delete" data-id="${post.id}" aria-label="Delete plan">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>
              </button>
            </div>
          ` : ''}
        </div>
        <h2 class="card-destination">${post.destination}</h2>
        <div class="card-dates">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
          <span>${post.dates}</span>
        </div>
        <p class="card-description">${post.description}</p>
        <div class="card-footer">
          <span class="card-interests">${post.interested_count} interested</span>
          ${isOwner ? '' : `
            <button class="btn-join-request ${isRequested ? 'requested' : ''}" data-id="${post.id}" ${isRequested ? 'disabled' : ''}>
              ${isRequested ? 'Requested ✓' : 'Join Request'}
            </button>
          `}
        </div>
      `;

      communityPostsList.appendChild(card);
    });

    // Bind Join Requests click listeners
    const joinBtns = communityPostsList.querySelectorAll('.btn-join-request');
    joinBtns.forEach(btn => {
      btn.addEventListener('click', (e) => {
        const id = e.target.getAttribute('data-id');
        const activeEmail = localStorage.getItem('ACTIVE_EMAIL') || 'aarav.mehta@gmail.com';
        
        e.target.disabled = true;
        
        const fd = new FormData();
        fd.append('id', id);
        fd.append('requester_name', activeUserName);
        fd.append('requester_email', activeEmail);

        fetch('http://localhost/TravelBuddybackend/community.php?action=join', {
          method: 'POST',
          body: fd
        })
        .then(res => res.json())
        .then(data => {
          if (data.status === 'success') {
            localStorage.setItem(`requested_post_${id}`, 'true');
            e.target.classList.add('requested');
            e.target.innerText = 'Requested ✓';
            
            // Increment UI interested count
            const countSpan = e.target.closest('.feed-card').querySelector('.card-interests');
            const currentInt = parseInt(countSpan.innerText) || 0;
            countSpan.innerText = `${currentInt + 1} interested`;
            
            showToast('Join Request Sent!');
            addAlertItem('Join Request Sent', `You requested to join a travel buddy's planned trip.`, 'join');
          } else {
            e.target.disabled = false;
            showToast('Could not submit join request.');
          }
        })
        .catch(() => {
          e.target.disabled = false;
          showToast('Network error submitting join request.');
        });
      });
    });

    // Bind Delete Post listener
    const deleteBtns = communityPostsList.querySelectorAll('.btn-card-action.delete');
    deleteBtns.forEach(btn => {
      btn.addEventListener('click', (e) => {
        const id = e.target.closest('button').getAttribute('data-id');
        if (!confirm('Are you sure you want to delete this travel plan?')) return;

        const fd = new FormData();
        fd.append('id', id);

        fetch('http://localhost/TravelBuddybackend/community.php?action=delete', {
          method: 'POST',
          body: fd
        })
        .then(res => res.json())
        .then(data => {
          if (data.status === 'success') {
            showToast('Post Deleted Successfully!');
            loadCommunityFeed();
          } else {
            showToast('Could not delete travel plan.');
          }
        })
        .catch(() => {
          showToast('Network error deleting plan.');
        });
      });
    });
  }

  // Filter explore list destination strings
  if (commSearchInput) {
    commSearchInput.addEventListener('input', () => {
      const q = commSearchInput.value.toLowerCase().trim();
      const filtered = loadedPosts.filter(p => p.destination.toLowerCase().includes(q));
      renderPostsFeed(filtered);
    });
  }

  // Create Post Submit Handler
  const createPostForm = document.getElementById('create-post-form');
  if (createPostForm) {
    createPostForm.addEventListener('submit', (e) => {
      e.preventDefault();
      
      const dest = document.getElementById('comm-destination').value.trim();
      const dates = document.getElementById('comm-dates').value.trim();
      const desc = document.getElementById('comm-description').value.trim();
      
      const activeUserName = localStorage.getItem('ACTIVE_USER_NAME') || 'Aarav Mehta';

      const fd = new FormData();
      fd.append('user_name', activeUserName);
      fd.append('destination', dest);
      fd.append('dates', dates);
      fd.append('description', desc);

      fetch('http://localhost/TravelBuddybackend/community.php?action=add', {
        method: 'POST',
        body: fd
      })
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success') {
          // Clear inputs
          createPostForm.reset();
          
          // Hide modal overlay
          createPostModal.classList.remove('visible');
          createPostModal.classList.add('hidden');
          
          showToast('Post Shared Successfully!');
          loadCommunityFeed();
          addAlertItem('Community Post Shared', `Successfully shared your travel plan to ${dest} with travel buddies!`, 'join');
        } else {
          showToast('Could not share travel plan.');
        }
      })
      .catch(() => {
        showToast('Network error sharing post.');
      });
    });
  }

  // Load received Join Requests lists
  const joinRequestsList = document.getElementById('join-requests-list');

  function loadJoinRequests() {
    joinRequestsList.innerHTML = `<div class="empty-feed-state"><div class="dot-pulse"></div><div class="dot-pulse"></div><div class="dot-pulse"></div></div>`;
    const activeUserName = localStorage.getItem('ACTIVE_USER_NAME') || 'Aarav Mehta';

    fetch(`http://localhost/TravelBuddybackend/community.php?action=get_requests&owner_name=${encodeURIComponent(activeUserName)}`)
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success' && data.data) {
          renderJoinRequests(data.data);
        } else {
          joinRequestsList.innerHTML = `<div class="empty-feed-state"><p>Could not load join requests.</p></div>`;
        }
      })
      .catch(() => {
        joinRequestsList.innerHTML = `<div class="empty-feed-state"><p>Error connecting to community requests.</p></div>`;
      });
  }

  function renderJoinRequests(requests) {
    if (requests.length === 0) {
      joinRequestsList.innerHTML = `
        <div class="empty-feed-state">
          <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"></path></svg>
          <p>No pending join requests.<br>When travel buddies request to join your offers, they will appear here!</p>
        </div>`;
      return;
    }

    joinRequestsList.innerHTML = '';
    
    requests.forEach(req => {
      // Pick cyclable color avatar
      const avatarId = (req.id % 5) + 1;
      const avatarBg = `avatar-bg-${avatarId}`;
      const initials = req.requester_name ? req.requester_name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase() : 'TB';

      const card = document.createElement('div');
      card.className = 'request-card';
      card.setAttribute('data-id', req.id);

      card.innerHTML = `
        <div class="card-profile">
          <div class="profile-avatar ${avatarBg}">${initials}</div>
          <div class="profile-info">
            <span class="profile-name">${req.requester_name}</span>
            <span class="profile-rating">${req.requester_email}</span>
          </div>
        </div>
        <p class="request-destination">Wants to join your trip to <strong>${req.destination}</strong></p>
        <div class="request-actions">
          <button class="btn-accept" data-id="${req.id}">Accept</button>
          <button class="btn-decline" data-id="${req.id}">Decline</button>
        </div>
      `;

      joinRequestsList.appendChild(card);
    });

    // Accept / Decline handlers
    const actionBtns = joinRequestsList.querySelectorAll('.btn-accept, .btn-decline');
    actionBtns.forEach(btn => {
      btn.addEventListener('click', (e) => {
        const id = e.target.getAttribute('data-id');
        const isAccept = e.target.classList.contains('btn-accept');
        const status = isAccept ? 'accepted' : 'declined';
        
        e.target.disabled = true;

        const fd = new FormData();
        fd.append('request_id', id);
        fd.append('status', status);

        fetch('http://localhost/TravelBuddybackend/community.php?action=respond_request', {
          method: 'POST',
          body: fd
        })
        .then(res => res.json())
        .then(data => {
          if (data.status === 'success') {
            showToast(`Join Request ${isAccept ? 'Accepted!' : 'Declined!'}`);
            loadJoinRequests();
          } else {
            e.target.disabled = false;
            showToast('Could not process decision.');
          }
        })
        .catch(() => {
          e.target.disabled = false;
          showToast('Network error processing decision.');
        });
      });
    });
  }

  /* ==========================================================================
     My Trips Integration Logic (Tab Switching, Dynamic Rendering, API Sync)
     ========================================================================== */
  const navHome = document.getElementById('navHome');
  const navTrips = document.getElementById('navTrips');
  const navSearch = document.getElementById('navSearch');
  const navAlerts = document.getElementById('navAlerts');
  const navProfile = document.getElementById('navProfile');

  const homeTabContent = document.getElementById('home-tab-content');
  const tripsTabContent = document.getElementById('trips-tab-content');
  
  const dbHeader = document.querySelector('.db-header');
  const dbSearchBar = document.getElementById('llSearchBar');

  const alertsTabContent = document.getElementById('alerts-tab-content');
  const profileTabContent = document.getElementById('profile-tab-content');
  const searchTabContent = document.getElementById('search-tab-content');

  // Handle switching to Home tab
  function switchToHomeTab(e) {
    if (e) e.preventDefault();
    
    // Set active class on bottom nav item
    document.querySelectorAll('.bottom-nav .nav-item').forEach(item => item.classList.remove('active'));
    if (navHome) navHome.classList.add('active');

    // Show Home content and header components
    if (homeTabContent) homeTabContent.classList.remove('hidden');
    if (tripsTabContent) tripsTabContent.classList.add('hidden');
    if (alertsTabContent) alertsTabContent.classList.add('hidden');
    if (profileTabContent) profileTabContent.classList.add('hidden');
    if (searchTabContent) searchTabContent.classList.add('hidden');
    if (dbHeader) dbHeader.style.display = 'flex';
    if (dbSearchBar) dbSearchBar.style.display = 'flex';
  }

  // Handle switching to Trips tab
  function switchToTripsTab(e) {
    if (e) e.preventDefault();

    // Set active class on bottom nav item
    document.querySelectorAll('.bottom-nav .nav-item').forEach(item => item.classList.remove('active'));
    if (navTrips) navTrips.classList.add('active');

    // Hide Home content and header components, show Trips tab content
    if (homeTabContent) homeTabContent.classList.add('hidden');
    if (tripsTabContent) tripsTabContent.classList.remove('hidden');
    if (alertsTabContent) alertsTabContent.classList.add('hidden');
    if (profileTabContent) profileTabContent.classList.add('hidden');
    if (searchTabContent) searchTabContent.classList.add('hidden');
    if (dbHeader) dbHeader.style.display = 'none';
    if (dbSearchBar) dbSearchBar.style.display = 'none';

    // Load trips from backend
    loadPlannedTrips();
  }

  // Handle switching to Alerts tab
  function switchToAlertsTab(e) {
    if (e) e.preventDefault();

    // Set active class on bottom nav item
    document.querySelectorAll('.bottom-nav .nav-item').forEach(item => item.classList.remove('active'));
    if (navAlerts) navAlerts.classList.add('active');

    // Hide other tabs, show Alerts tab
    if (homeTabContent) homeTabContent.classList.add('hidden');
    if (tripsTabContent) tripsTabContent.classList.add('hidden');
    if (alertsTabContent) alertsTabContent.classList.remove('hidden');
    if (profileTabContent) profileTabContent.classList.add('hidden');
    if (searchTabContent) searchTabContent.classList.add('hidden');
    if (dbHeader) dbHeader.style.display = 'none';
    if (dbSearchBar) dbSearchBar.style.display = 'none';

    // Fetch alerts from XAMPP backend
    loadAlerts();
  }

  // Handle switching to Profile tab
  function switchToProfileTab(e) {
    if (e) e.preventDefault();

    // Set active class on bottom nav item
    document.querySelectorAll('.bottom-nav .nav-item').forEach(item => item.classList.remove('active'));
    if (navProfile) navProfile.classList.add('active');

    // Hide other tabs, show Profile tab
    if (homeTabContent) homeTabContent.classList.add('hidden');
    if (tripsTabContent) tripsTabContent.classList.add('hidden');
    if (alertsTabContent) alertsTabContent.classList.add('hidden');
    if (profileTabContent) profileTabContent.classList.remove('hidden');
    if (searchTabContent) searchTabContent.classList.add('hidden');
    if (dbHeader) dbHeader.style.display = 'none';
    if (dbSearchBar) dbSearchBar.style.display = 'none';

    // Fetch active session info
    loadProfileDetails();
  }

  // Handle switching to Search tab
  function switchToSearchTab(e) {
    if (e) e.preventDefault();

    // Set active class on bottom nav item
    document.querySelectorAll('.bottom-nav .nav-item').forEach(item => item.classList.remove('active'));
    if (navSearch) navSearch.classList.add('active');

    // Hide other tabs, show Search tab
    if (homeTabContent) homeTabContent.classList.add('hidden');
    if (tripsTabContent) tripsTabContent.classList.add('hidden');
    if (alertsTabContent) alertsTabContent.classList.add('hidden');
    if (profileTabContent) profileTabContent.classList.add('hidden');
    if (searchTabContent) searchTabContent.classList.remove('hidden');
    if (dbHeader) dbHeader.style.display = 'none';
    if (dbSearchBar) dbSearchBar.style.display = 'none';

    // Populate initial trending destinations list
    filterSearchDestinations('');
  }

  if (navHome) navHome.addEventListener('click', switchToHomeTab);
  if (navTrips) navTrips.addEventListener('click', switchToTripsTab);
  if (navAlerts) navAlerts.addEventListener('click', switchToAlertsTab);
  if (navProfile) navProfile.addEventListener('click', switchToProfileTab);
  if (navSearch) navSearch.addEventListener('click', switchToSearchTab);

  /* ==========================================================================
     Search Screen & Trending Destinations Logic (Parity with SearchActivity.kt)
     ========================================================================== */
  const allDestinations = [
    "Mahabalipuram", "Ooty", "Pondicherry", "Madurai", "Bali, Indonesia", 
    "Tokyo, Japan", "Santorini, Greece", "Dubai, UAE", "Paris, France", 
    "Iceland", "Maldives", "New York, USA", "London, UK", "Rome, Italy", 
    "Singapore", "Sydney, Australia", "Kodaikanal", "Munnar", "Hampi"
  ];

  const countryMap = {
    "america": ["New York, USA", "Los Angeles", "Grand Canyon", "Las Vegas", "Chicago"],
    "usa": ["New York, USA", "Los Angeles", "Grand Canyon", "Las Vegas", "Chicago"],
    "india": ["New Delhi", "Mumbai", "Taj Mahal", "Goa", "Jaipur", "Kerala"],
    "tamil nadu": ["Chennai", "Madurai", "Ooty", "Mahabalipuram", "Kodaikanal", "Coimbatore"],
    "france": ["Paris", "Nice", "Lyon", "Marseille", "Bordeaux"],
    "uk": ["London", "Edinburgh", "Manchester", "Liverpool"],
    "australia": ["Sydney", "Melbourne", "Great Barrier Reef", "Perth"]
  };

  function filterSearchDestinations(query) {
    const container = document.getElementById('trending-list-container');
    if (!container) return;

    const lowerQuery = query.toLowerCase().trim();
    let filtered = [];

    if (!lowerQuery) {
      filtered = allDestinations;
    } else if (countryMap[lowerQuery]) {
      filtered = countryMap[lowerQuery];
    } else {
      filtered = allDestinations.filter(d => d.toLowerCase().includes(lowerQuery));
      if (filtered.length === 0) {
        // Replicate matches.add(query) behavior when custom text has no match
        const formattedQuery = query.replace(/\b\w/g, c => c.toUpperCase());
        filtered.push(formattedQuery);
      }
    }

    renderTrendingDestinations(filtered);
  }

  function renderTrendingDestinations(items) {
    const container = document.getElementById('trending-list-container');
    if (!container) return;

    container.innerHTML = '';
    items.forEach(item => {
      const row = document.createElement('button');
      row.className = 'trending-destination-item';
      row.innerHTML = `
        <div class="trending-item-icon-wrapper" aria-hidden="true">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
            <circle cx="12" cy="10" r="3"></circle>
          </svg>
        </div>
        <span class="trending-item-name">${item}</span>
      `;

      row.addEventListener('click', () => {
        openTripDetails(item);
      });

      container.appendChild(row);
    });
  }

  // Bind input keyup filter actions
  const searchTabInput = document.getElementById('search-tab-input-id');
  if (searchTabInput) {
    searchTabInput.addEventListener('input', (e) => {
      filterSearchDestinations(e.target.value);
    });
  }

  // Hook home search bar input to switch tabs dynamically
  const mainSearchInput = document.getElementById('search-input-id');
  if (mainSearchInput) {
    mainSearchInput.addEventListener('input', (e) => {
      const q = e.target.value;
      switchToSearchTab();
      if (searchTabInput) {
        searchTabInput.value = q;
        searchTabInput.focus();
        filterSearchDestinations(q);
      }
      e.target.value = ''; // clear home input
    });
  }

  /* ==========================================================================
     Alerts & Notifications System Logic (Parity with SQLite Android client)
     ========================================================================== */
  function loadAlerts() {
    const container = document.getElementById('alerts-list-container');
    const emptyState = document.getElementById('llEmptyStateAlerts');
    if (!container) return;

    container.innerHTML = `
      <div class="empty-alerts-state">
        <div class="dot-pulse"></div>
      </div>`;
    if (emptyState) emptyState.classList.add('hidden');

    const email = localStorage.getItem('ACTIVE_EMAIL') || 'aarav.mehta@gmail.com';
    const alertsUrl = `${BASE_URL}/alerts.php?action=get&email=${encodeURIComponent(email)}`;

    fetch(alertsUrl)
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success' && data.data) {
          renderAlertsList(data.data);
        } else {
          container.innerHTML = '<div class="empty-alerts-state"><p>Could not load notifications.</p></div>';
        }
      })
      .catch(() => {
        container.innerHTML = '<div class="empty-alerts-state"><p>Error connecting to notifications center.</p></div>';
      });
  }

  function renderAlertsList(alerts) {
    const container = document.getElementById('alerts-list-container');
    const emptyState = document.getElementById('llEmptyStateAlerts');
    if (!container) return;

    if (alerts.length === 0) {
      container.innerHTML = '';
      if (emptyState) emptyState.classList.remove('hidden');
      return;
    }

    if (emptyState) emptyState.classList.add('hidden');
    container.innerHTML = '';

    alerts.forEach(alert => {
      let iconColor = '#6B7280'; // fallback gray
      let typeIcon = '';

      switch (alert.type.toLowerCase()) {
        case 'trip':
          iconColor = '#10B981'; // Emerald Green
          typeIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>`;
          break;
        case 'hotel_success':
          iconColor = '#06B6D4'; // Teal
          typeIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22"></polyline></svg>`;
          break;
        case 'hotel_failure':
          iconColor = '#EF4444'; // Red
          typeIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>`;
          break;
        case 'weather':
          iconColor = '#F59E0B'; // Amber Yellow
          typeIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>`;
          break;
        case 'travel':
          iconColor = '#3B82F6'; // Indigo Blue
          typeIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M22 2L11 13"></path><path d="M22 2l-7 20-4-9-9-4 20-7z"></path></svg>`;
          break;
        case 'join':
          iconColor = '#8B5CF6'; // Purple
          typeIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>`;
          break;
        default:
          typeIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>`;
          break;
      }

      // Format timestamp relatively (Epoch Millis)
      const diff = Date.now() - alert.timestamp;
      let relativeTime = 'Just now';
      if (diff > 0) {
        const secs = Math.floor(diff / 1000);
        const mins = Math.floor(secs / 60);
        const hours = Math.floor(mins / 60);
        const days = Math.floor(hours / 24);

        if (days > 0) {
          relativeTime = `${days}d ago`;
        } else if (hours > 0) {
          relativeTime = `${hours}h ago`;
        } else if (mins > 0) {
          relativeTime = `${mins}m ago`;
        } else if (secs > 5) {
          relativeTime = `${secs}s ago`;
        }
      }

      const item = document.createElement('div');
      item.className = 'alert-card-item';
      item.innerHTML = `
        <div class="alert-card-left">
          <div class="alert-icon-avatar" style="background-color: ${iconColor};">
            ${typeIcon}
          </div>
          <div class="alert-card-text">
            <h4 class="alert-card-title">${alert.title}</h4>
            <p class="alert-card-msg">${alert.message}</p>
            <span class="alert-card-time">${relativeTime}</span>
          </div>
        </div>
        <button class="btn-delete-alert" data-id="${alert.id}" aria-label="Delete Notification">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </button>
      `;

      item.querySelector('.btn-delete-alert').addEventListener('click', (e) => {
        e.stopPropagation();
        const id = e.currentTarget.getAttribute('data-id');
        deleteAlert(id);
      });

      container.appendChild(item);
    });
  }

  function deleteAlert(id) {
    const deleteUrl = `${BASE_URL}/alerts.php?action=delete&id=${id}`;
    fetch(deleteUrl)
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success') {
          showToast('Notification deleted!');
          loadAlerts();
        }
      });
  }

  function clearAllAlerts() {
    const email = localStorage.getItem('ACTIVE_EMAIL') || 'aarav.mehta@gmail.com';
    const clearUrl = `${BASE_URL}/alerts.php?action=clear&email=${encodeURIComponent(email)}`;
    
    fetch(clearUrl)
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success') {
          showToast('All notifications cleared!');
          loadAlerts();
        }
      });
  }

  function addAlertItem(title, message, type) {
    const email = localStorage.getItem('ACTIVE_EMAIL') || 'aarav.mehta@gmail.com';
    const addUrl = `${BASE_URL}/alerts.php?action=add`;

    const fd = new FormData();
    fd.append('email', email);
    fd.append('title', title);
    fd.append('message', message);
    fd.append('type', type);

    fetch(addUrl, {
      method: 'POST',
      body: fd
    }).catch(() => {
      console.warn('Could not sync alert trigger to XAMPP backend');
    });
  }

  // Clear all alerts button trigger
  const btnClearAllAlerts = document.getElementById('btnClearAllAlerts');
  if (btnClearAllAlerts) {
    btnClearAllAlerts.addEventListener('click', clearAllAlerts);
  }

  /* ==========================================================================
     Profile Dashboard Logic
     ========================================================================== */
  function loadProfileDetails() {
    const activeUserName = localStorage.getItem('ACTIVE_USER_NAME') || 'simats';
    const activeEmail = localStorage.getItem('ACTIVE_EMAIL') || 'simats@gmail.com';

    // Populate profile card view
    const tvProfileName = document.getElementById('tvProfileName');
    const tvProfileEmail = document.getElementById('tvProfileEmail');
    const profileAvatarInitials = document.getElementById('profile-avatar-initials');

    if (tvProfileName) tvProfileName.textContent = activeUserName;
    if (tvProfileEmail) tvProfileEmail.textContent = activeEmail;

    // Create avatar initials
    let init = 'S';
    if (activeUserName) {
      const parts = activeUserName.split(' ');
      let initStr = '';
      if (parts.length > 0 && parts[0]) initStr += parts[0][0].toUpperCase();
      if (parts.length > 1 && parts[1]) initStr += parts[1][0].toUpperCase();
      init = initStr || 'S';
    }
    if (profileAvatarInitials) profileAvatarInitials.textContent = init;

    // Populate My Information screen elements
    const infoScreenAvatar = document.getElementById('info-screen-avatar');
    const infoScreenName = document.getElementById('info-screen-name');
    const infoScreenEmail = document.getElementById('info-screen-email');
    if (infoScreenAvatar) infoScreenAvatar.textContent = init;
    if (infoScreenName) infoScreenName.textContent = activeUserName;
    if (infoScreenEmail) infoScreenEmail.textContent = activeEmail;

    // Populate modal values (for backup/modal access if triggered)
    const infoValName = document.getElementById('info-val-name');
    const infoValEmail = document.getElementById('info-val-email');
    if (infoValName) infoValName.textContent = activeUserName;
    if (infoValEmail) infoValEmail.textContent = activeEmail;
  }

  // Helper helper to transition screens cleanly
  function navigateToProfileScreen(screenId) {
    // Hide main dashboard section
    const dashboardSection = document.getElementById('dashboard-section');
    if (dashboardSection) {
      dashboardSection.classList.remove('visible');
      dashboardSection.classList.add('hidden');
    }

    // Show target section
    const targetScreen = document.getElementById(screenId);
    if (targetScreen) {
      targetScreen.classList.remove('hidden');
      targetScreen.classList.add('visible');
    }
  }

  function navigateBackToProfileDashboard(screenId) {
    // Hide active sub-screen section
    const targetScreen = document.getElementById(screenId);
    if (targetScreen) {
      targetScreen.classList.remove('visible');
      targetScreen.classList.add('hidden');
    }

    // Show main dashboard section
    const dashboardSection = document.getElementById('dashboard-section');
    if (dashboardSection) {
      dashboardSection.classList.remove('hidden');
      dashboardSection.classList.add('visible');
    }
  }

  // Profile Option Screen transitions click listeners
  const btnMyInfo = document.getElementById('btnMyInfo');
  if (btnMyInfo) {
    btnMyInfo.addEventListener('click', () => {
      loadProfileDetails();
      navigateToProfileScreen('profile-info-section');
    });
  }

  const btnInfoBack = document.getElementById('btn-info-back');
  if (btnInfoBack) {
    btnInfoBack.addEventListener('click', () => {
      navigateBackToProfileDashboard('profile-info-section');
    });
  }

  // Join Requests transition and dynamic loading
  const btnProfileRequests = document.getElementById('btnProfileRequests');
  if (btnProfileRequests) {
    btnProfileRequests.addEventListener('click', () => {
      navigateToProfileScreen('profile-requests-section');
      loadPendingRequests();
    });
  }

  const btnRequestsBack = document.getElementById('btn-requests-back');
  if (btnRequestsBack) {
    btnRequestsBack.addEventListener('click', () => {
      navigateBackToProfileDashboard('profile-requests-section');
    });
  }

  function loadPendingRequests() {
    const ownerName = localStorage.getItem('ACTIVE_USER_NAME') || 'simats';
    const reqContainer = document.getElementById('screen-requests-container');
    if (!reqContainer) return;

    fetch(`${BASE_URL}/community.php?action=get_requests&owner_name=${encodeURIComponent(ownerName)}`)
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success' && data.data && data.data.length > 0) {
          reqContainer.innerHTML = '';
          data.data.forEach(req => {
            const card = document.createElement('div');
            card.className = 'info-fields-card';
            card.style.cursor = 'pointer';
            card.style.marginBottom = '16px';
            card.innerHTML = `
              <div style="display:flex; justify-content:space-between; align-items:center;">
                <div>
                  <h4 style="color:var(--white); margin:0 0 4px 0; font-size:16px;">${req.requester_name}</h4>
                  <p style="color:rgba(255,255,255,0.4); margin:0; font-size:12px;">Wants to join: <strong style="color:#3B82F6;">${req.destination}</strong></p>
                  <p style="color:rgba(255,255,255,0.5); margin:4px 0 0 0; font-size:13px; text-decoration:underline;">${req.requester_email}</p>
                </div>
                <div style="display:flex; gap:8px;">
                  <button class="btn-decision accept-btn" data-id="${req.id}" style="background:#10B981; border:none; color:var(--white); padding:8px 12px; border-radius:8px; cursor:pointer; font-weight:700; font-size:12px;">Accept</button>
                  <button class="btn-decision decline-btn" data-id="${req.id}" style="background:#EF4444; border:none; color:var(--white); padding:8px 12px; border-radius:8px; cursor:pointer; font-weight:700; font-size:12px;">Decline</button>
                </div>
              </div>
            `;

            // Action triggers for Accept/Decline decisions
            card.querySelector('.accept-btn').addEventListener('click', (e) => {
              e.stopPropagation();
              handleRequestDecision(req.id, true);
            });
            card.querySelector('.decline-btn').addEventListener('click', (e) => {
              e.stopPropagation();
              handleRequestDecision(req.id, false);
            });

            // Click card to draft email inquiry (Android parity requirement in screenshot subtitle)
            card.addEventListener('click', () => {
              window.location.href = `mailto:${req.requester_email}?subject=TravelBuddy: Regarding your request to join trip to ${encodeURIComponent(req.destination)}`;
            });

            reqContainer.appendChild(card);
          });
        } else {
          // Centered empty state matching the user's screenshot exactly!
          reqContainer.innerHTML = `
            <div class="empty-requests-container">
              <p class="empty-requests-text">No pending join requests</p>
            </div>
          `;
        }
      })
      .catch(() => {
        reqContainer.innerHTML = `
          <div class="empty-requests-container">
            <p class="empty-requests-text">No pending join requests</p>
          </div>
        `;
      });
  }

  function handleRequestDecision(reqId, isAccept) {
    const fd = new FormData();
    fd.append('request_id', reqId);
    fd.append('status', isAccept ? 'accepted' : 'declined');

    fetch(`${BASE_URL}/community.php?action=respond_request`, {
      method: 'POST',
      body: fd
    })
    .then(res => res.json())
    .then(data => {
      if (data.status === 'success') {
        showToast(`Join Request ${isAccept ? 'Accepted!' : 'Declined!'}`);
        loadPendingRequests();
      } else {
        showToast('Could not process decision.');
      }
    })
    .catch(() => {
      showToast('Network error processing decision.');
    });
  }

  // Privacy & Policy transition
  const btnPrivacyPolicy = document.getElementById('btnPrivacyPolicy');
  if (btnPrivacyPolicy) {
    btnPrivacyPolicy.addEventListener('click', () => {
      navigateToProfileScreen('profile-privacy-section');
    });
  }

  const btnPrivacyBack = document.getElementById('btn-privacy-back');
  if (btnPrivacyBack) {
    btnPrivacyBack.addEventListener('click', () => {
      navigateBackToProfileDashboard('profile-privacy-section');
    });
  }

  // Help & Support transition
  const btnHelpSupport = document.getElementById('btnHelpSupport');
  if (btnHelpSupport) {
    btnHelpSupport.addEventListener('click', () => {
      navigateToProfileScreen('profile-support-section');
    });
  }

  const btnSupportBack = document.getElementById('btn-support-back');
  if (btnSupportBack) {
    btnSupportBack.addEventListener('click', () => {
      navigateBackToProfileDashboard('profile-support-section');
    });
  }

  // Open Contact support modal overlay inside screen CTA button click
  const btnContactUs = document.getElementById('btnContactUs');
  const supportModal = document.getElementById('support-modal');
  const btnSupportClose = document.getElementById('btn-support-close');

  if (btnContactUs && supportModal) {
    btnContactUs.addEventListener('click', () => {
      supportModal.classList.remove('hidden');
      supportModal.classList.add('visible');
    });
  }
  if (btnSupportClose && supportModal) {
    btnSupportClose.addEventListener('click', () => {
      supportModal.classList.remove('visible');
      supportModal.classList.add('hidden');
    });
  }

  // Reset Password Dialog overlay handlers
  const btnResetPassword = document.getElementById('btnResetPassword');
  const resetPasswordModal = document.getElementById('reset-password-modal');
  const btnResetPasswordClose = document.getElementById('btn-reset-password-close');

  if (btnResetPassword && resetPasswordModal) {
    btnResetPassword.addEventListener('click', () => {
      const activeEmail = localStorage.getItem('ACTIVE_EMAIL') || 'simats@gmail.com';
      const resetEmailInput = document.getElementById('reset-val-email');
      if (resetEmailInput) resetEmailInput.value = activeEmail;

      resetPasswordModal.classList.remove('hidden');
      resetPasswordModal.classList.add('visible');
    });
  }

  if (btnResetPasswordClose && resetPasswordModal) {
    btnResetPasswordClose.addEventListener('click', () => {
      resetPasswordModal.classList.remove('visible');
      resetPasswordModal.classList.add('hidden');
    });
  }

  // Reset Password Form submission backend integration
  const resetPasswordForm = document.getElementById('reset-password-form');
  if (resetPasswordForm) {
    resetPasswordForm.addEventListener('submit', (e) => {
      e.preventDefault();

      const email = document.getElementById('reset-val-email').value.trim();
      const password = document.getElementById('reset-val-password').value;

      if (!password) {
        showToast('Please enter a new password.');
        return;
      }

      // Prepare payload
      const fd = new FormData();
      fd.append('email', email);
      fd.append('password', password);

      fetch(`${BASE_URL}/reset_password.php`, {
        method: 'POST',
        body: fd
      })
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success') {
          showToast('Password reset successful!');
          if (resetPasswordModal) {
            resetPasswordModal.classList.remove('visible');
            resetPasswordModal.classList.add('hidden');
          }
          document.getElementById('reset-val-password').value = '';
        } else {
          showToast(data.message || 'Failed to update password.');
        }
      })
      .catch(() => {
        showToast('Server is experiencing high traffic right now, please try again.');
      });
    });
  }

  const supportForm = document.getElementById('support-request-form');
  if (supportForm) {
    supportForm.addEventListener('submit', (e) => {
      e.preventDefault();
      supportForm.reset();
      if (supportModal) {
        supportModal.classList.remove('visible');
        supportModal.classList.add('hidden');
      }
      showToast('Support ticket submitted successfully! We will contact you soon.');
    });
  }

  const btnLogOut = document.getElementById('btnLogOut');
  if (btnLogOut) {
    btnLogOut.addEventListener('click', () => {
      localStorage.removeItem('ACTIVE_EMAIL');
      localStorage.removeItem('ACTIVE_USER_NAME');
      showToast('Logged out successfully!');
      setTimeout(() => {
        window.location.reload();
      }, 800);
    });
  }

  // Map destination keywords to premium Unsplash travel images
  function getDestinationImage(destination) {
    const dest = destination.toLowerCase();
    if (dest.includes('mahabalipuram')) {
      return 'https://images.unsplash.com/photo-1581012771300-224937651c42?auto=format&fit=crop&w=600&q=80';
    }
    if (dest.includes('goa')) {
      return 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=600&q=80';
    }
    if (dest.includes('nellore')) {
      return 'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=600&q=80';
    }
    if (dest.includes('delhi')) {
      return 'https://images.unsplash.com/photo-1587474260584-136574528ed5?auto=format&fit=crop&w=600&q=80';
    }
    if (dest.includes('mumbai')) {
      return 'https://images.unsplash.com/photo-1570168007204-dfb528c6958f?auto=format&fit=crop&w=600&q=80';
    }
    if (dest.includes('taj mahal') || dest.includes('agra')) {
      return 'https://images.unsplash.com/photo-1564507592333-c60657eea523?auto=format&fit=crop&w=600&q=80';
    }
    if (dest.includes('kerala')) {
      return 'https://images.unsplash.com/photo-1593693397690-362cb9666fc2?auto=format&fit=crop&w=600&q=80';
    }
    // Elegant fallback travel images
    return 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?auto=format&fit=crop&w=600&q=80';
  }

  // Fetch Planned Trips from get_trips.php backend API
  function loadPlannedTrips() {
    const container = document.getElementById('trips-list-container');
    if (!container) return;

    container.innerHTML = `
      <div class="empty-feed-state">
        <div class="dot-pulse"></div>
        <div class="dot-pulse"></div>
        <div class="dot-pulse"></div>
      </div>`;
    
    const email = localStorage.getItem('ACTIVE_EMAIL') || 'aarav.mehta@gmail.com';
    const getTripsUrl = `${BASE_URL}/get_trips.php?email=${encodeURIComponent(email)}`;

    fetch(getTripsUrl)
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success' && data.data) {
          renderPlannedTrips(data.data);
        } else {
          container.innerHTML = `<div class="empty-feed-state"><p>Could not load planned trips.</p></div>`;
        }
      })
      .catch(() => {
        container.innerHTML = `<div class="empty-feed-state"><p>Error connecting to planned trips backend.</p></div>`;
      });
  }

  // Render trips feed cards
  function renderPlannedTrips(trips) {
    const container = document.getElementById('trips-list-container');
    if (!container) return;

    if (trips.length === 0) {
      container.innerHTML = `
        <div class="empty-feed-state">
          <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
          <p>No planned trips found.<br>Tap the plus button above to add one!</p>
        </div>`;
      return;
    }

    container.innerHTML = '';
    trips.forEach((trip, index) => {
      const imgUrl = getDestinationImage(trip.destination);
      const badgeText = (index === 0) ? 'Active' : 'Planned';

      const card = document.createElement('div');
      card.className = 'trip-card';
      card.setAttribute('data-destination', trip.destination);
      
      card.innerHTML = `
        <img src="${imgUrl}" class="trip-card-image" alt="${trip.destination}">
        <div class="trip-card-overlay"></div>
        <span class="trip-badge">${badgeText}</span>
        <button class="btn-trip-delete" data-destination="${trip.destination}" aria-label="Delete Trip">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="3 6 5 6 21 6"></polyline>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
          </svg>
        </button>
        <h3 class="trip-title">${trip.destination}</h3>
        
        <div class="trip-card-footer">
          <div class="trip-footer-left">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
              <line x1="16" y1="2" x2="16" y2="6"></line>
              <line x1="8" y1="2" x2="8" y2="6"></line>
              <line x1="3" y1="10" x2="21" y2="10"></line>
            </svg>
            <span>Planned Trip</span>
          </div>
          <button class="btn-trip-location" data-destination="${trip.destination}">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
              <circle cx="12" cy="10" r="3"></circle>
            </svg>
          </button>
        </div>
      `;

      container.appendChild(card);
    });

    // Make the entire card click open trip details
    container.querySelectorAll('.trip-card').forEach(card => {
      card.addEventListener('click', (e) => {
        const dest = e.currentTarget.getAttribute('data-destination');
        openTripDetails(dest);
      });
    });

    // Location pin opens trip details too
    container.querySelectorAll('.btn-trip-location').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const dest = e.currentTarget.getAttribute('data-destination');
        openTripDetails(dest);
      });
    });

    // Delete dynamic handlers
    container.querySelectorAll('.btn-trip-delete').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const dest = e.currentTarget.getAttribute('data-destination');
        deletePlannedTrip(dest);
      });
    });
  }

  // Add Trip Modal Overlay controls
  const addTripModal = document.getElementById('add-trip-modal');
  const fabAddTrip = document.getElementById('fabAddTrip');
  const btnAddTripClose = document.getElementById('btn-add-trip-close-id');
  const addTripForm = document.getElementById('add-trip-form');

  if (fabAddTrip && addTripModal) {
    fabAddTrip.addEventListener('click', () => {
      addTripModal.classList.remove('hidden');
      addTripModal.classList.add('visible');
      const inputField = document.getElementById('trip-destination');
      if (inputField) inputField.focus();
    });
  }

  if (btnAddTripClose && addTripModal) {
    btnAddTripClose.addEventListener('click', () => {
      addTripModal.classList.remove('visible');
      addTripModal.classList.add('hidden');
    });
  }

  // Handle Add Trip Submit
  if (addTripForm) {
    addTripForm.addEventListener('submit', (e) => {
      e.preventDefault();
      
      const destField = document.getElementById('trip-destination');
      const dest = destField ? destField.value.trim() : '';
      if (!dest) return;

      const email = localStorage.getItem('ACTIVE_EMAIL') || 'aarav.mehta@gmail.com';
      const randomImageRes = Math.floor(Math.random() * 5) + 1;

      const fd = new FormData();
      fd.append('email', email);
      fd.append('destination', dest);
      fd.append('image_res', randomImageRes);

      fetch(`${BASE_URL}/add_trip.php`, {
        method: 'POST',
        body: fd
      })
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success') {
          addTripForm.reset();
          if (addTripModal) {
            addTripModal.classList.remove('visible');
            addTripModal.classList.add('hidden');
          }
          showToast('Trip Added Successfully!');
          loadPlannedTrips();
          addAlertItem('Trip Planned', `Successfully planned a new trip to ${dest}!`, 'trip');
        } else {
          showToast(data.message || 'Could not add planned trip.');
        }
      })
      .catch(() => {
        showToast('Network error adding planned trip.');
      });
    });
  }

  // Handle planned trip deletion
  function deletePlannedTrip(destination) {
    if (!confirm(`Are you sure you want to delete your trip to "${destination}"?`)) return;

    const email = localStorage.getItem('ACTIVE_EMAIL') || 'aarav.mehta@gmail.com';
    const fd = new FormData();
    fd.append('email', email);
    fd.append('destination', destination);

    fetch(`${BASE_URL}/delete_trip.php`, {
      method: 'POST',
      body: fd
    })
    .then(res => res.json())
    .then(data => {
      if (data.status === 'success') {
        showToast('Trip Deleted Successfully!');
        loadPlannedTrips();
        addAlertItem('Trip Deleted', `Removed trip to ${destination} from your planning list.`, 'hotel_failure');
      } else {
        showToast(data.message || 'Could not delete planned trip.');
      }
    })
    .catch(() => {
      showToast('Network error deleting planned trip.');
    });
  }

  // ==========================================================================
  // Web Trip Details View Page and Sub-Planning Overlays Integration
  // ==========================================================================
  const tripDetailsSection = document.getElementById('trip-details-section');
  // dashboardSection is already declared in the outer scope

  function openTripDetails(destination) {
    if (!tripDetailsSection || !dashboardSection) return;

    // Transition panels
    dashboardSection.classList.add('hidden');
    tripDetailsSection.classList.remove('hidden');

    // Populate static fields instantly
    document.getElementById('details-destination-title').textContent = destination;
    const heroImg = document.getElementById('details-hero-img');
    if (heroImg) heroImg.src = getDestinationImage(destination);

    // Dynamic fields loaders
    document.getElementById('details-rating-num').textContent = '4.5';
    document.getElementById('details-description').textContent = 'Loading destination overview...';
    document.getElementById('details-travel-methods').textContent = 'Finding travel methods...';
    document.getElementById('details-budget-min').textContent = 'Min: ₹--';
    document.getElementById('details-budget-max').textContent = 'Max: ₹--';
    document.getElementById('details-recommended-places').innerHTML = `
      <div class="empty-feed-state" style="padding: 10px;">
        <div class="dot-pulse"></div>
      </div>`;
    document.getElementById('details-hotels-list-id').innerHTML = `
      <li style="opacity: 0.5;">Loading premium hotels...</li>`;

    // 1. Fetch details from place_details.php backend (powered by Gemini AI)
    const detailsUrl = `${BASE_URL}/place_details.php?place=${encodeURIComponent(destination)}`;
    fetch(detailsUrl)
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success' && data.data) {
          const info = data.data;

          document.getElementById('details-rating-num').textContent = info.rating || '4.5';
          document.getElementById('details-description').textContent = info.description || 'No detailed overview available at this moment.';
          document.getElementById('details-travel-methods').textContent = info.travel_methods || 'No direct routes found.';
          document.getElementById('details-budget-min').textContent = `Min: ₹${info.budget_min || '2500'}`;
          document.getElementById('details-budget-max').textContent = `Max: ₹${info.budget_max || '15000'}`;

          // Recommended places rendering
          const recContainer = document.getElementById('details-recommended-places');
          recContainer.innerHTML = '';
          const recPlaces = info.recommended_places || [];
          if (recPlaces.length === 0) {
            recContainer.innerHTML = '<p class="details-paragraph" style="opacity:0.5">No recommendations found.</p>';
          } else {
            recPlaces.forEach((place, index) => {
              const placeImg = getDestinationImage(place);
              const rate = (4.3 + (index * 0.1)).toFixed(1);
              const rCard = document.createElement('div');
              rCard.className = 'recommended-place-card';
              rCard.innerHTML = `
                <img src="${placeImg}" alt="${place}">
                <div class="place-card-overlay"></div>
                <span class="place-card-rating">★ ${rate}</span>
                <span class="place-card-name">${place}</span>
              `;
              recContainer.appendChild(rCard);
            });
          }

          // Hotels listing rendering
          const hotelsContainer = document.getElementById('details-hotels-list-id');
          hotelsContainer.innerHTML = '';
          const hotelsList = info.hotels || [];
          if (hotelsList.length === 0) {
            hotelsContainer.innerHTML = '<li style="opacity:0.5">No nearby hotels listed.</li>';
          } else {
            hotelsList.forEach(hotel => {
              const li = document.createElement('li');
              li.textContent = hotel;
              hotelsContainer.appendChild(li);
            });
          }

          // Setup maps route navigator
          const mapBtn = document.getElementById('btn-details-route');
          if (mapBtn) {
            const newMapBtn = mapBtn.cloneNode(true);
            mapBtn.parentNode.replaceChild(newMapBtn, mapBtn);
            
            const lat = info.latitude || 12.9716;
            const lng = info.longitude || 77.5946;
            newMapBtn.addEventListener('click', () => {
              window.open(`https://www.google.com/maps/search/?api=1&query=${lat},${lng}`, '_blank');
            });
          }
        } else {
          showToast('Failed to load destination insights.');
        }
      })
      .catch(() => {
        showToast('Network error loading place details.');
      });

    // 2. Setup Delete Action
    const deleteBtn = document.getElementById('btn-details-delete-trip');
    if (deleteBtn) {
      const newDeleteBtn = deleteBtn.cloneNode(true);
      deleteBtn.parentNode.replaceChild(newDeleteBtn, deleteBtn);
      newDeleteBtn.addEventListener('click', () => {
        deletePlannedTrip(destination);
        closeTripDetails();
      });
    }

    // 3. Bind planning trigger cards
    setupPlanningButtons(destination);
  }

  function closeTripDetails() {
    if (!tripDetailsSection || !dashboardSection) return;
    tripDetailsSection.classList.add('hidden');
    dashboardSection.classList.remove('hidden');
    loadPlannedTrips();
  }

  // Bind toolbar back click
  const detailsBackBtn = document.getElementById('btn-details-back-id');
  if (detailsBackBtn) {
    detailsBackBtn.addEventListener('click', closeTripDetails);
  }

  // Setup sub features overlays and dynamic queries
  function setupPlanningButtons(trip) {
    // ── Packing: navigate to full-screen page ──
    const packBtn = document.getElementById('btn-pack-list');
    if (packBtn) {
      const newPackBtn = packBtn.cloneNode(true);
      packBtn.parentNode.replaceChild(newPackBtn, packBtn);
      newPackBtn.addEventListener('click', () => openPackingPage(trip));
    }

    // ── Budget: navigate to full-screen page ──
    const budgetBtn = document.getElementById('btn-budget-list');
    if (budgetBtn) {
      const newBudgetBtn = budgetBtn.cloneNode(true);
      budgetBtn.parentNode.replaceChild(newBudgetBtn, budgetBtn);
      newBudgetBtn.addEventListener('click', () => openBudgetPage(trip));
    }

    // ── Tickets: navigate to full-screen page ──
    const ticketBtn = document.getElementById('btn-ticket-list');
    if (ticketBtn) {
      const newTicketBtn = ticketBtn.cloneNode(true);
      ticketBtn.parentNode.replaceChild(newTicketBtn, ticketBtn);
      newTicketBtn.addEventListener('click', () => openTicketsPage(trip));
    }

    // ── Remaining overlays (hotels, weather, buddy) ──
    const triggers = [
      { id: 'btn-hotel-list', overlay: 'hotels-overlay', load: () => loadHotelsOverlay(trip) },
      { id: 'btn-weather-list', overlay: 'weather-overlay', load: () => loadWeatherOverlay(trip) },
      { id: 'btn-buddy-plan', overlay: 'buddy-plan-overlay', load: () => openBuddyPlanOverlay(trip) }
    ];

    triggers.forEach(item => {
      const btn = document.getElementById(item.id);
      if (btn) {
        const newBtn = btn.cloneNode(true);
        btn.parentNode.replaceChild(newBtn, btn);
        newBtn.addEventListener('click', () => {
          const overlay = document.getElementById(item.overlay);
          if (overlay) {
            overlay.classList.remove('hidden');
            overlay.classList.add('visible');
            item.load();
          }
        });
      }
    });
  }

  // Overlay Close Handlers
  const overlayClosers = [
    { btn: 'btn-tickets-close', overlay: 'tickets-overlay' },
    { btn: 'btn-hotels-close', overlay: 'hotels-overlay' },
    { btn: 'btn-budget-close', overlay: 'budget-overlay' },
    { btn: 'btn-weather-close', overlay: 'weather-overlay' },
    { btn: 'btn-buddy-plan-close', overlay: 'buddy-plan-overlay' }
  ];

  overlayClosers.forEach(closer => {
    const btn = document.getElementById(closer.btn);
    if (btn) {
      btn.addEventListener('click', () => {
        const overlay = document.getElementById(closer.overlay);
        if (overlay) {
          overlay.classList.remove('visible');
          overlay.classList.add('hidden');
        }
      });
    }
  });

  // ==========================================================================
  // Plan with Buddy – Open overlay and handle community post submission
  // ==========================================================================
  function openBuddyPlanOverlay(destination) {
    // Pre-fill destination field
    const destInput = document.getElementById('buddy-plan-destination');
    if (destInput) destInput.value = destination;

    // Reset other fields
    const datesInput = document.getElementById('buddy-plan-dates');
    const descInput = document.getElementById('buddy-plan-desc');
    if (datesInput) datesInput.value = '';
    if (descInput) descInput.value = '';
  }

  const buddyPlanForm = document.getElementById('buddy-plan-form');
  if (buddyPlanForm) {
    buddyPlanForm.addEventListener('submit', (e) => {
      e.preventDefault();

      const dest = document.getElementById('buddy-plan-destination').value.trim();
      const dates = document.getElementById('buddy-plan-dates').value.trim();
      const desc = document.getElementById('buddy-plan-desc').value.trim();

      if (!dest || !desc) {
        showToast('Please fill in destination and description.');
        return;
      }

      const activeUserName = localStorage.getItem('ACTIVE_USER_NAME') || 'Traveler';
      const activeEmail   = localStorage.getItem('ACTIVE_EMAIL')     || '';

      const submitBtn = document.getElementById('btn-buddy-plan-submit');
      if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Adding trip & sharing...';
      }

      // ── 1. Add destination to My Trips (INSERT IGNORE – safe if already exists) ──
      const tripFd = new FormData();
      tripFd.append('email', activeEmail);
      tripFd.append('destination', dest);
      tripFd.append('image_res', Math.floor(Math.random() * 5) + 1);

      const addTripReq = fetch(`${BASE_URL}/add_trip.php`, {
        method: 'POST',
        body: tripFd
      }).then(r => r.json());

      // ── 2. Post to community Trip Buddies feed ──
      const communityFd = new FormData();
      communityFd.append('user_name',   activeUserName);
      communityFd.append('destination', dest);
      communityFd.append('dates',       dates || 'Dates TBD');
      communityFd.append('description', desc);

      const communityReq = fetch(`${BASE_URL}/community.php?action=add`, {
        method: 'POST',
        body: communityFd
      }).then(r => r.json());

      // ── Fire both simultaneously ──
      Promise.all([addTripReq, communityReq])
        .then(([tripData, communityData]) => {
          if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Share to Trip Buddies';
          }

          const tripAdded      = tripData.status === 'success';
          const communityShared = communityData.status === 'success';

          if (communityShared || tripAdded) {
            // Close the overlay
            const overlay = document.getElementById('buddy-plan-overlay');
            if (overlay) {
              overlay.classList.remove('visible');
              overlay.classList.add('hidden');
            }
            buddyPlanForm.reset();

            if (tripAdded && communityShared) {
              showToast(`"${dest}" added to My Trips & shared to Trip Buddies! 🎉`);
              addAlertItem('Plan with Buddy', `Trip to ${dest} saved in My Trips and posted to the community feed!`, 'trip');
            } else if (tripAdded) {
              showToast(`"${dest}" added to My Trips!`);
              addAlertItem('Trip Added', `Trip to ${dest} added to My Trips.`, 'trip');
            } else {
              showToast('Shared to Trip Buddies! 🎉');
              addAlertItem('Shared to Trip Buddies', `Your trip to ${dest} is now visible in the community feed!`, 'join');
            }

            // Refresh the trips list so the new trip appears immediately
            loadPlannedTrips();
          } else {
            showToast(communityData.message || tripData.message || 'Something went wrong. Please try again.');
          }
        })
        .catch(() => {
          if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Share to Trip Buddies';
          }
          showToast('Network error. Please try again.');
        });
    });
  }

  // ==========================================================================
  // Packing List – Full-Screen Page Navigation & Backend
  // ==========================================================================
  let _packingCurrentTrip = '';

  function openPackingPage(trip) {
    _packingCurrentTrip = trip;
    const section = document.getElementById('packing-section');
    const tripDetails = document.getElementById('trip-details-section');
    if (!section) return;

    // Set destination title
    const titleEl = document.getElementById('packing-dest-title');
    if (titleEl) titleEl.textContent = trip;

    // Hide trip details, show packing page
    if (tripDetails) tripDetails.classList.add('hidden');
    section.classList.remove('hidden');

    // Load items
    loadPackingList(trip);
  }

  function closePackingPage() {
    const section = document.getElementById('packing-section');
    const tripDetails = document.getElementById('trip-details-section');
    if (section) section.classList.add('hidden');
    if (tripDetails) tripDetails.classList.remove('hidden');
  }

  // Back button
  const btnPackingBack = document.getElementById('btn-packing-back');
  if (btnPackingBack) {
    btnPackingBack.addEventListener('click', closePackingPage);
  }

  // Add Item bar
  const packingAddBtn = document.getElementById('packing-add-btn');
  const packingAddInput = document.getElementById('packing-add-input');
  const packingCatSelect = document.getElementById('packing-cat-select');

  function handleAddPackingItem() {
    const item = packingAddInput ? packingAddInput.value.trim() : '';
    const cat = packingCatSelect ? packingCatSelect.value : 'General';
    if (!item || !_packingCurrentTrip) return;

    fetch(`${BASE_URL}/packing.php?action=add&trip=${encodeURIComponent(_packingCurrentTrip)}&item=${encodeURIComponent(item)}&category=${encodeURIComponent(cat)}`)
      .then(res => res.json())
      .then(data => {
        if (data.status === 'success') {
          if (packingAddInput) packingAddInput.value = '';
          loadPackingList(_packingCurrentTrip);
        }
      });
  }

  if (packingAddBtn) {
    packingAddBtn.addEventListener('click', handleAddPackingItem);
  }
  if (packingAddInput) {
    packingAddInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') { e.preventDefault(); handleAddPackingItem(); }
    });
  }

  // Packing Checklist Core Backend Sync
  function loadPackingList(trip) {
    const container = document.getElementById('packing-items-list');
    if (!container) return;
    container.innerHTML = `
      <div class="packing-empty-state">
        <div class="dot-pulse"></div>
      </div>`;

    fetch(`${BASE_URL}/packing.php?action=get&trip=${encodeURIComponent(trip)}`)
      .then(res => res.json())
      .then(res => {
        if (res.status === 'success' && res.data) {
          renderPackingList(res.data, trip);
        } else {
          container.innerHTML = '<div class="packing-empty-state"><p>Could not load packing list.</p></div>';
        }
      })
      .catch(() => {
        container.innerHTML = '<div class="packing-empty-state"><p>Network error loading checklist.</p></div>';
      });
  }

  function updatePackingProgress(items) {
    const total  = items.length;
    const packed = items.filter(i => i.is_packed).length;
    const pct    = total === 0 ? 0 : Math.round((packed / total) * 100);
    const circumference = 175.93; // 2π × 28

    const labelEl  = document.getElementById('packing-progress-label');
    const pctEl    = document.getElementById('packing-ring-pct');
    const ringFill = document.getElementById('packing-ring-fill');

    if (labelEl)  labelEl.textContent  = `${packed} of ${total} items packed`;
    if (pctEl)    pctEl.textContent    = `${pct}%`;
    if (ringFill) {
      const offset = circumference - (pct / 100) * circumference;
      ringFill.style.strokeDashoffset = offset;
      ringFill.style.stroke = pct === 100 ? '#10B981' : '#3B82F6';
    }
  }

  function renderPackingList(items, trip) {
    const container = document.getElementById('packing-items-list');
    if (!container) return;
    container.innerHTML = '';

    // Update progress ring
    updatePackingProgress(items);

    if (items.length === 0) {
      container.innerHTML = `
        <div class="packing-empty-state">
          <svg xmlns="http://www.w3.org/2000/svg" width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
            <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
          </svg>
          <p>No items yet.<br>Add your first packing item below!</p>
        </div>`;
      return;
    }

    const catClassMap = {
      'Clothes': 'cat-clothes',
      'Documents': 'cat-documents',
      'Gadgets': 'cat-gadgets',
      'General': 'cat-general'
    };

    items.forEach(item => {
      const row = document.createElement('div');
      row.className = `packing-item-row${item.is_packed ? ' is-packed' : ''}`;
      const catClass = catClassMap[item.category] || 'cat-general';

      row.innerHTML = `
        <div class="packing-checkbox${item.is_packed ? ' checked' : ''}"></div>
        <span class="packing-item-name">${item.item_name}</span>
        <span class="packing-item-cat ${catClass}">${item.category}</span>
        <button class="packing-delete-btn" aria-label="Delete">
          <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="3 6 5 6 21 6"></polyline>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
          </svg>
        </button>
      `;

      // Toggle checkbox
      row.querySelector('.packing-checkbox').addEventListener('click', () => {
        const nextStatus = item.is_packed ? 0 : 1;
        fetch(`${BASE_URL}/packing.php?action=toggle&trip=${encodeURIComponent(trip)}&id=${item.id}&status=${nextStatus}`)
          .then(r => r.json())
          .then(d => { if (d.status === 'success') loadPackingList(trip); });
      });

      // Delete button
      row.querySelector('.packing-delete-btn').addEventListener('click', (e) => {
        e.stopPropagation();
        fetch(`${BASE_URL}/packing.php?action=delete&trip=${encodeURIComponent(trip)}&id=${item.id}`)
          .then(r => r.json())
          .then(d => { if (d.status === 'success') loadPackingList(trip); });
      });

      container.appendChild(row);
    });
  }

  // ==========================================================================
  // Ticket Booking – Full-Screen Page + Razorpay Payment
  // ==========================================================================
  let _ticketsCurrentTrip  = '';
  let _ticketSelectedPrice = 0;  // in paise * 100 for Razorpay
  let _ticketSeats         = 1;
  let _ticketBasePrice     = 0;  // per-seat price (numeric)
  let _ticketType          = '';

  function openTicketsPage(trip) {
    _ticketsCurrentTrip = trip;
    const section    = document.getElementById('tickets-section');
    const tripDetails = document.getElementById('trip-details-section');
    if (!section) return;

    // Set title + from label
    const titleEl = document.getElementById('tickets-page-title');
    if (titleEl) titleEl.textContent = `Tickets to ${trip}`;
    const fromEl = document.getElementById('tickets-from-label');
    if (fromEl) fromEl.textContent = 'From: Chennai, India';

    if (tripDetails) tripDetails.classList.add('hidden');
    section.classList.remove('hidden');

    // Close sheet if open from previous session
    closeTicketSheet();

    loadTicketBooking(trip);
  }

  function closeTicketsPage() {
    const section    = document.getElementById('tickets-section');
    const tripDetails = document.getElementById('trip-details-section');
    if (section) section.classList.add('hidden');
    if (tripDetails) tripDetails.classList.remove('hidden');
    closeTicketSheet();
  }

  const btnTicketsBack = document.getElementById('btn-tickets-back');
  if (btnTicketsBack) btnTicketsBack.addEventListener('click', closeTicketsPage);

  // ── Booking Sheet Open / Close ──
  function openTicketSheet(ticket) {
    _ticketType      = ticket.type;
    const priceStr   = (ticket.price || '').replace(/[^\d]/g, '');
    _ticketBasePrice = parseInt(priceStr) || 0;
    _ticketSeats     = 1;

    const sheet    = document.getElementById('ticket-booking-sheet');
    const backdrop = document.getElementById('ticket-sheet-backdrop');
    const titleEl  = document.getElementById('ticket-sheet-title');
    const countEl  = document.getElementById('ticket-seat-count');
    const totalEl  = document.getElementById('ticket-total-amt');
    const dateInput = document.getElementById('ticket-date-input');

    // Set today as min date
    const today = new Date().toISOString().split('T')[0];
    if (dateInput) { dateInput.min = today; dateInput.value = ''; }

    if (titleEl) titleEl.textContent = `Book ${ticket.type} Ticket`;
    if (countEl) countEl.textContent = '1';
    updateTicketTotal();

    // Reset class to Standard
    const stdRadio = document.querySelector('input[name="ticket-class"][value="standard"]');
    if (stdRadio) stdRadio.checked = true;

    if (sheet)    { sheet.classList.remove('hidden'); sheet.style.visibility = 'visible'; sheet.style.pointerEvents = 'auto'; }
    if (backdrop) backdrop.classList.remove('hidden');
  }

  function closeTicketSheet() {
    const sheet    = document.getElementById('ticket-booking-sheet');
    const backdrop = document.getElementById('ticket-sheet-backdrop');
    if (sheet)    sheet.classList.add('hidden');
    if (backdrop) backdrop.classList.add('hidden');
  }

  // Close sheet when backdrop clicked
  document.getElementById('ticket-sheet-backdrop')?.addEventListener('click', closeTicketSheet);

  // ── Seat Counter ──
  function updateTicketTotal() {
    const premiumRadio = document.querySelector('input[name="ticket-class"][value="premium"]');
    const isPremium    = premiumRadio && premiumRadio.checked;
    const multiplier   = isPremium ? 1.5 : 1;
    const total        = Math.round(_ticketBasePrice * _ticketSeats * multiplier);
    _ticketSelectedPrice = total;
    const totalEl = document.getElementById('ticket-total-amt');
    if (totalEl) totalEl.textContent = `₹${total.toLocaleString('en-IN')}`;
  }

  document.getElementById('ticket-seat-minus')?.addEventListener('click', () => {
    if (_ticketSeats > 1) {
      _ticketSeats--;
      const el = document.getElementById('ticket-seat-count');
      if (el) el.textContent = _ticketSeats;
      updateTicketTotal();
    }
  });

  document.getElementById('ticket-seat-plus')?.addEventListener('click', () => {
    if (_ticketSeats < 10) {
      _ticketSeats++;
      const el = document.getElementById('ticket-seat-count');
      if (el) el.textContent = _ticketSeats;
      updateTicketTotal();
    }
  });

  document.querySelectorAll('input[name="ticket-class"]').forEach(radio => {
    radio.addEventListener('change', updateTicketTotal);
  });

  // ── Razorpay Payment ──
  document.getElementById('ticket-pay-btn')?.addEventListener('click', () => {
    const dateInput = document.getElementById('ticket-date-input');
    if (!dateInput || !dateInput.value) {
      showToast('Please select a departure date.');
      return;
    }
    if (_ticketSelectedPrice <= 0) {
      showToast('No ticket selected.');
      return;
    }

    const userName  = localStorage.getItem('ACTIVE_USER_NAME') || 'Traveler';
    const userEmail = localStorage.getItem('ACTIVE_EMAIL')     || 'traveler@example.com';
    const classType = document.querySelector('input[name="ticket-class"]:checked')?.value || 'standard';

    // Razorpay test key – demo mode
    const options = {
      key: 'rzp_test_SqOZwDnHPrqJm0',
      amount: _ticketSelectedPrice * 100, // in paise
      currency: 'INR',
      name: 'TravelBuddy',
      description: `${_ticketType} ticket to ${_ticketsCurrentTrip} (${_ticketSeats} seat${_ticketSeats > 1 ? 's' : ''}, ${classType})`,
      image: 'https://i.imgur.com/n5tjHFD.png',
      handler: function(response) {
        closeTicketSheet();
        const payId = response.razorpay_payment_id || 'DEMO-' + Date.now();
        showToast(`✅ Ticket booked! Payment ID: ${payId}`);
        addAlertItem(
          'Ticket Booked ✔',
          `${_ticketType} ticket to ${_ticketsCurrentTrip} on ${dateInput.value}. ID: ${payId}`,
          'travel'
        );
      },
      prefill: { name: userName, email: userEmail },
      theme: { color: '#3B82F6' },
      modal: {
        ondismiss: () => showToast('Payment cancelled.')
      }
    };

    // Check if Razorpay SDK loaded; if not, simulate demo payment
    if (typeof window.Razorpay !== 'undefined') {
      const rzp = new window.Razorpay(options);
      rzp.open();
    } else {
      // Demo fallback (no internet / SDK blocked)
      closeTicketSheet();
      const demoId = 'DEMO-' + Date.now();
      showToast(`✅ [Demo] Ticket booked! Ref: ${demoId}`);
      addAlertItem(
        'Ticket Booked ✔',
        `${_ticketType} ticket to ${_ticketsCurrentTrip} on ${dateInput.value}. Ref: ${demoId}`,
        'travel'
      );
    }
  });

  // Ticket Booking – Load & Render
  function loadTicketBooking(trip) {
    const container = document.getElementById('tickets-transport-list');
    if (!container) return;
    container.innerHTML = '<div class="ticket-loading-state"><div class="dot-pulse"></div></div>';

    fetch(`${BASE_URL}/tickets.php?from=Chennai&to=${encodeURIComponent(trip)}`)
      .then(res => res.json())
      .then(res => {
        if (res.status === 'success' && res.data) {
          renderTicketsList(res.data);
        } else {
          container.innerHTML = '<div class="ticket-loading-state"><p style="color:rgba(255,255,255,0.4)">Could not fetch ticket options.</p></div>';
        }
      })
      .catch(() => {
        container.innerHTML = '<div class="ticket-loading-state"><p style="color:rgba(255,255,255,0.4)">Network error.</p></div>';
      });
  }

  function renderTicketsList(tickets) {
    const container = document.getElementById('tickets-transport-list');
    if (!container) return;
    container.innerHTML = '';

    const iconMap = {
      bus:     { emoji: '🚌', cls: 'bus-sq' },
      cab:     { emoji: '🚗', cls: 'cab-sq' },
      train:   { emoji: '🚂', cls: 'train-sq' },
      airways: { emoji: '✈️', cls: 'airways-sq' }
    };

    tickets.forEach(ticket => {
      const typeKey = (ticket.type || '').toLowerCase();
      const icon = iconMap[typeKey] || iconMap['cab'];

      const card = document.createElement('div');
      card.className = 'transport-card';
      card.innerHTML = `
        <div class="transport-icon-sq ${icon.cls}">${icon.emoji}</div>
        <div class="transport-info">
          <span class="transport-type">${ticket.type}</span>
          <span class="transport-duration">${ticket.duration}</span>
        </div>
        <div class="transport-right">
          <span class="transport-price">${ticket.price}</span>
          <button class="btn-transport-book">Book Now</button>
        </div>
      `;

      card.querySelector('.btn-transport-book').addEventListener('click', () => {
        openTicketSheet(ticket);
      });

      container.appendChild(card);
    });
  }

  // ==========================================================================
  // Hotel Booking Multi-Step Flow + Razorpay Payment Gateway
  // ==========================================================================
  let _hotelBooking = {
    hotelName: '',
    hotelPricePerNight: 0,
    roomType: 'AC',          // 'AC' or 'Non-AC'
    acMultiplier: 1.2,       // AC rooms cost 20% more
    nights: 1,
    guests: 1,
    checkinDate: '',
    tripName: '',
    currentStep: 1,
  };

  function _hotelCalcTotal() {
    const base = _hotelBooking.hotelPricePerNight;
    const multiplier = (_hotelBooking.roomType === 'AC') ? _hotelBooking.acMultiplier : 1;
    return Math.round(base * multiplier * _hotelBooking.nights * _hotelBooking.guests);
  }

  function _hotelFormatINR(n) {
    return '₹' + n.toLocaleString('en-IN');
  }

  function _hotelGotoStep(step) {
    _hotelBooking.currentStep = step;
    const steps = ['hotel-step-1', 'hotel-step-2', 'hotel-step-3'];
    const titles = ['Room Type', 'Stay Duration', 'Payment'];
    steps.forEach((id, i) => {
      const el = document.getElementById(id);
      if (el) { el.classList.toggle('hidden', i + 1 !== step); }
    });
    const badge = document.getElementById('hotel-step-badge');
    const title = document.getElementById('hotel-booking-modal-title');
    const bar   = document.getElementById('hotel-step-bar');
    if (badge) badge.textContent = `Step ${step} of 3`;
    if (title) title.textContent = titles[step - 1];
    if (bar)   bar.style.width   = `${(step / 3) * 100}%`;
  }

  function _hotelUpdateStep1UI() {
    const base = _hotelBooking.hotelPricePerNight;
    const mult = (_hotelBooking.roomType === 'AC') ? _hotelBooking.acMultiplier : 1;
    const price = Math.round(base * mult);
    const priceEl = document.getElementById('hotel-step1-price');
    if (priceEl) priceEl.textContent = _hotelFormatINR(price) + ' / night';

    // Toggle selected class on room cards
    const acCard    = document.getElementById('hotel-btn-ac');
    const nonacCard = document.getElementById('hotel-btn-nonac');
    if (acCard)    acCard.classList.toggle('selected',    _hotelBooking.roomType === 'AC');
    if (nonacCard) nonacCard.classList.toggle('selected', _hotelBooking.roomType === 'Non-AC');
  }

  function _hotelUpdateStep2UI() {
    const daysEl   = document.getElementById('hotel-days-count');
    const peopleEl = document.getElementById('hotel-people-count');
    const sumType  = document.getElementById('hotel-sum-type');
    const sumNights = document.getElementById('hotel-sum-nights');
    const sumTotal  = document.getElementById('hotel-sum-total');

    if (daysEl)    daysEl.textContent   = _hotelBooking.nights;
    if (peopleEl)  peopleEl.textContent = _hotelBooking.guests;
    if (sumType)   sumType.textContent  = _hotelBooking.roomType + ' Room';
    if (sumNights) sumNights.textContent = `${_hotelBooking.nights} night${_hotelBooking.nights > 1 ? 's' : ''} × ${_hotelBooking.guests} guest${_hotelBooking.guests > 1 ? 's' : ''}`;
    if (sumTotal)  sumTotal.textContent  = _hotelFormatINR(_hotelCalcTotal());
  }

  function _hotelPopulateStep3() {
    const total = _hotelCalcTotal();
    document.getElementById('hfs-hotel-name').textContent  = _hotelBooking.hotelName;
    document.getElementById('hfs-room-type').textContent   = _hotelBooking.roomType + ' Room';
    document.getElementById('hfs-checkin').textContent     = _hotelBooking.checkinDate || 'Not set';
    document.getElementById('hfs-duration').textContent    = `${_hotelBooking.nights} night${_hotelBooking.nights > 1 ? 's' : ''}`;
    document.getElementById('hfs-guests').textContent      = `${_hotelBooking.guests} guest${_hotelBooking.guests > 1 ? 's' : ''}`;
    document.getElementById('hfs-total').textContent       = _hotelFormatINR(total);
  }

  function openHotelBookingModal(hotel, trip) {
    // Initialize state
    _hotelBooking.hotelName          = hotel.name;
    _hotelBooking.hotelPricePerNight = parseInt((hotel.price || '0').toString().replace(/[^\d]/g, '')) || 0;
    _hotelBooking.roomType   = 'AC';
    _hotelBooking.nights     = 1;
    _hotelBooking.guests     = 1;
    _hotelBooking.checkinDate = '';
    _hotelBooking.tripName   = trip;

    // Set hotel name display
    const nameDisplay = document.getElementById('hotel-booking-name-display');
    if (nameDisplay) nameDisplay.textContent = hotel.name;

    // Set check-in date min to today
    const checkinInput = document.getElementById('hotel-checkin-date');
    if (checkinInput) {
      const today = new Date().toISOString().split('T')[0];
      checkinInput.min   = today;
      checkinInput.value = '';
    }

    _hotelGotoStep(1);
    _hotelUpdateStep1UI();

    // Show modal
    const modal = document.getElementById('hotel-booking-modal');
    if (modal) {
      modal.classList.remove('hidden');
      modal.classList.add('visible');
    }
  }

  function closeHotelBookingModal() {
    const modal = document.getElementById('hotel-booking-modal');
    if (modal) {
      modal.classList.remove('visible');
      modal.classList.add('hidden');
    }
  }

  // --- Close Button ---
  document.getElementById('btn-hotel-booking-close')?.addEventListener('click', closeHotelBookingModal);

  // --- Step 1: Room type selection ---
  document.getElementById('hotel-btn-ac')?.addEventListener('click', () => {
    _hotelBooking.roomType = 'AC';
    _hotelUpdateStep1UI();
  });
  document.getElementById('hotel-btn-nonac')?.addEventListener('click', () => {
    _hotelBooking.roomType = 'Non-AC';
    _hotelUpdateStep1UI();
  });
  document.getElementById('hotel-step1-next')?.addEventListener('click', () => {
    _hotelGotoStep(2);
    _hotelUpdateStep2UI();
  });

  // --- Step 2: Nights/Guests counters ---
  document.getElementById('hotel-days-minus')?.addEventListener('click', () => {
    if (_hotelBooking.nights > 1) { _hotelBooking.nights--; _hotelUpdateStep2UI(); }
  });
  document.getElementById('hotel-days-plus')?.addEventListener('click', () => {
    if (_hotelBooking.nights < 30) { _hotelBooking.nights++; _hotelUpdateStep2UI(); }
  });
  document.getElementById('hotel-people-minus')?.addEventListener('click', () => {
    if (_hotelBooking.guests > 1) { _hotelBooking.guests--; _hotelUpdateStep2UI(); }
  });
  document.getElementById('hotel-people-plus')?.addEventListener('click', () => {
    if (_hotelBooking.guests < 10) { _hotelBooking.guests++; _hotelUpdateStep2UI(); }
  });
  document.getElementById('hotel-checkin-date')?.addEventListener('change', (e) => {
    _hotelBooking.checkinDate = e.target.value;
  });
  document.getElementById('hotel-step2-back')?.addEventListener('click', () => _hotelGotoStep(1));
  document.getElementById('hotel-step2-next')?.addEventListener('click', () => {
    if (!_hotelBooking.checkinDate) {
      showToast('Please select a check-in date.');
      return;
    }
    _hotelPopulateStep3();
    _hotelGotoStep(3);
  });

  // --- Step 3: Back ---
  document.getElementById('hotel-step3-back')?.addEventListener('click', () => _hotelGotoStep(2));

  // --- PAY NOW → Razorpay ---
  document.getElementById('hotel-pay-now-btn')?.addEventListener('click', () => {
    const total = _hotelCalcTotal();
    if (total <= 0) { showToast('Invalid booking amount.'); return; }

    const userName  = localStorage.getItem('ACTIVE_USER_NAME') || 'Traveler';
    const userEmail = localStorage.getItem('ACTIVE_EMAIL')     || 'traveler@example.com';

    const description = `${_hotelBooking.roomType} Room @ ${_hotelBooking.hotelName} | ${_hotelBooking.nights} nights | ${_hotelBooking.guests} guests | Check-in: ${_hotelBooking.checkinDate}`;

    const options = {
      key: 'rzp_test_SqOZwDnHPrqJm0',
      amount: total * 100,  // paise
      currency: 'INR',
      name: 'TravelBuddy Hotels',
      description: description,
      image: 'https://i.imgur.com/n5tjHFD.png',
      handler: function(response) {
        closeHotelBookingModal();
        const payId = response.razorpay_payment_id || 'DEMO-' + Date.now();
        showToast(`✅ Hotel Booked! Payment ID: ${payId}`);
        addAlertItem(
          'Hotel Booked ✔',
          `${_hotelBooking.roomType} room at ${_hotelBooking.hotelName} for ${_hotelBooking.nights} night${_hotelBooking.nights > 1 ? 's' : ''}, ${_hotelBooking.guests} guest${_hotelBooking.guests > 1 ? 's' : ''}. Check-in: ${_hotelBooking.checkinDate}. ID: ${payId}`,
          'hotel_success'
        );
      },
      prefill: { name: userName, email: userEmail },
      theme: { color: '#8B5CF6' },
      modal: { ondismiss: () => showToast('Payment cancelled.') }
    };

    if (typeof window.Razorpay !== 'undefined') {
      const rzp = new window.Razorpay(options);
      rzp.open();
    } else {
      // Demo fallback
      closeHotelBookingModal();
      const demoId = 'DEMO-' + Date.now();
      showToast(`✅ [Demo] Hotel Booked! Ref: ${demoId}`);
      addAlertItem(
        'Hotel Booked ✔',
        `${_hotelBooking.roomType} room at ${_hotelBooking.hotelName} for ${_hotelBooking.nights} night(s), ${_hotelBooking.guests} guest(s). Check-in: ${_hotelBooking.checkinDate}. Ref: ${demoId}`,
        'hotel_success'
      );
    }
  });

  // Hotels Overlay Core Integration
  function loadHotelsOverlay(trip) {
    const container = document.getElementById('hotels-overlay-list');
    container.innerHTML = `
      <div class="empty-feed-state">
        <div class="dot-pulse"></div>
      </div>`;

    fetch(`${BASE_URL}/hotels.php?place=${encodeURIComponent(trip)}`)
      .then(res => res.json())
      .then(data => {
        if (Array.isArray(data)) {
          renderHotelsOverlay(data, trip);
        } else {
          container.innerHTML = '<div class="empty-feed-state"><p>Could not fetch nearby hotels.</p></div>';
        }
      })
      .catch(() => {
        container.innerHTML = '<div class="empty-feed-state"><p>Error connecting to hotels backend.</p></div>';
      });
  }

  function renderHotelsOverlay(hotels, trip) {
    const container = document.getElementById('hotels-overlay-list');
    container.innerHTML = '';

    hotels.forEach(hotel => {
      const card = document.createElement('div');
      card.className = 'hotel-overlay-card';
      card.innerHTML = `
        <div class="hotel-card-header">
          <h4>${hotel.name}</h4>
          <span class="hotel-stars">★ ${hotel.rating}</span>
        </div>
        <p class="hotel-desc">${hotel.description}</p>
        <div class="hotel-card-footer">
          <div class="hotel-price">
            <span class="hotel-price-lbl">Starting Price</span>
            <span class="hotel-price-val">₹${hotel.price} / night</span>
          </div>
          <div class="hotel-card-actions">
            <button class="btn-hotel-call" title="Call Hotel" data-phone="${hotel.contact}">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path>
              </svg>
            </button>
            <button class="btn-hotel-book">Book Stay</button>
          </div>
        </div>
      `;

      card.querySelector('.btn-hotel-call').addEventListener('click', (e) => {
        const ph = e.currentTarget.getAttribute('data-phone');
        showToast(`Calling hotel partner: ${ph}`);
      });

      // ── Open multi-step hotel booking modal ──
      card.querySelector('.btn-hotel-book').addEventListener('click', () => {
        openHotelBookingModal(hotel, trip || '');
      });

      container.appendChild(card);
    });
  }

  // ==========================================================================
  // Budget Tracker – Full-Screen Page Navigation & Backend
  // ==========================================================================
  let _budgetCurrentTrip = '';

  function openBudgetPage(trip) {
    _budgetCurrentTrip = trip;
    const section    = document.getElementById('budget-section');
    const tripDetails = document.getElementById('trip-details-section');
    if (!section) return;
    if (tripDetails) tripDetails.classList.add('hidden');
    section.classList.remove('hidden');
    loadBudgetTracker(trip);
  }

  function closeBudgetPage() {
    const section    = document.getElementById('budget-section');
    const tripDetails = document.getElementById('trip-details-section');
    if (section) section.classList.add('hidden');
    if (tripDetails) tripDetails.classList.remove('hidden');
  }

  const btnBudgetBack = document.getElementById('btn-budget-back');
  if (btnBudgetBack) btnBudgetBack.addEventListener('click', closeBudgetPage);

  // Edit budget (pencil) → open limit modal
  const bfsEditBtn = document.getElementById('bfs-edit-btn');
  if (bfsEditBtn) {
    bfsEditBtn.addEventListener('click', () => {
      const modal = document.getElementById('bfs-limit-modal');
      if (modal) modal.classList.remove('hidden');
    });
  }
  document.getElementById('bfs-limit-cancel')?.addEventListener('click', () => {
    document.getElementById('bfs-limit-modal')?.classList.add('hidden');
  });
  document.getElementById('bfs-limit-save')?.addEventListener('click', () => {
    const val = parseFloat(document.getElementById('bfs-limit-input').value);
    if (isNaN(val) || val < 0) { showToast('Enter a valid amount'); return; }
    fetch(`${BASE_URL}/budget.php?action=set_limit&trip=${encodeURIComponent(_budgetCurrentTrip)}&limit=${val}`)
      .then(r => r.json())
      .then(d => {
        if (d.status === 'success') {
          document.getElementById('bfs-limit-input').value = '';
          document.getElementById('bfs-limit-modal').classList.add('hidden');
          loadBudgetTracker(_budgetCurrentTrip);
        }
      });
  });

  // ADD button → open expense modal
  const bfsAddBtn = document.getElementById('bfs-add-btn');
  if (bfsAddBtn) {
    bfsAddBtn.addEventListener('click', () => {
      const modal = document.getElementById('bfs-expense-modal');
      if (modal) modal.classList.remove('hidden');
    });
  }
  document.getElementById('bfs-exp-cancel')?.addEventListener('click', () => {
    document.getElementById('bfs-expense-modal')?.classList.add('hidden');
  });
  document.getElementById('bfs-exp-save')?.addEventListener('click', () => {
    const note = (document.getElementById('bfs-exp-note').value || '').trim();
    const amt  = parseFloat(document.getElementById('bfs-exp-amount').value);
    if (!note || isNaN(amt) || amt <= 0) { showToast('Enter description and amount'); return; }
    fetch(`${BASE_URL}/budget.php?action=add_expense&trip=${encodeURIComponent(_budgetCurrentTrip)}&amount=${amt}&note=${encodeURIComponent(note)}`)
      .then(r => r.json())
      .then(d => {
        if (d.status === 'success') {
          document.getElementById('bfs-exp-note').value   = '';
          document.getElementById('bfs-exp-amount').value = '';
          document.getElementById('bfs-expense-modal').classList.add('hidden');
          loadBudgetTracker(_budgetCurrentTrip);
        }
      });
  });

  function loadBudgetTracker(trip) {
    const expList = document.getElementById('bfs-expenses-list');
    if (expList) expList.innerHTML = '<div class="budget-empty-state"><div class="dot-pulse"></div></div>';

    fetch(`${BASE_URL}/budget.php?action=get&trip=${encodeURIComponent(trip)}`)
      .then(res => res.json())
      .then(res => {
        if (res.status === 'success') {
          renderBudgetTracker(res.total_budget, res.expenses, trip);
        }
      })
      .catch(() => {
        if (expList) expList.innerHTML = '<div class="budget-empty-state"><p>Network error loading budget.</p></div>';
      });
  }

  function renderBudgetTracker(limit, expenses, trip) {
    const totalSpent = expenses.reduce((acc, curr) => acc + curr.amount, 0);
    const remaining  = limit - totalSpent;
    const pct = limit > 0 ? Math.min((totalSpent / limit) * 100, 100) : 0;
    const isOver = limit > 0 && totalSpent > limit;

    // Update summary card
    const spentEl  = document.getElementById('bfs-spent-big');
    const limitLbl = document.getElementById('bfs-budget-label');
    const pctLbl   = document.getElementById('bfs-pct-lbl');
    const leftLbl  = document.getElementById('bfs-left-lbl');
    const barFill  = document.getElementById('bfs-bar-fill');

    if (spentEl)  spentEl.textContent  = `₹${totalSpent.toFixed(0)}`;
    if (limitLbl) limitLbl.textContent = `Budget: ₹${limit.toFixed(0)}`;
    if (pctLbl)   pctLbl.textContent   = `${Math.round(pct)}% used`;
    if (leftLbl) {
      leftLbl.textContent = isOver
        ? `₹${Math.abs(remaining).toFixed(0)} over budget`
        : `₹${remaining.toFixed(0)} left`;
      leftLbl.classList.toggle('over-budget', isOver);
    }
    if (barFill) {
      barFill.style.width = `${pct}%`;
      barFill.classList.toggle('over-budget', isOver);
    }

    // Expenses list
    const expList = document.getElementById('bfs-expenses-list');
    if (!expList) return;
    expList.innerHTML = '';

    if (expenses.length === 0) {
      expList.innerHTML = `
        <div class="budget-empty-state">
          <svg xmlns="http://www.w3.org/2000/svg" width="52" height="52" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <rect x="2" y="4" width="20" height="16" rx="2" ry="2"></rect>
            <line x1="12" y1="1" x2="12" y2="23"></line>
          </svg>
          <p>No expenses tracked yet.<br>Tap ADD to record one!</p>
        </div>`;
      return;
    }

    // Emoji icon map
    const emojiFor = (note) => {
      const n = note.toLowerCase();
      if (n.includes('food') || n.includes('dinner') || n.includes('lunch') || n.includes('meal')) return '🍔';
      if (n.includes('hotel') || n.includes('stay') || n.includes('room'))  return '🏨';
      if (n.includes('transport') || n.includes('taxi') || n.includes('bus') || n.includes('flight')) return '✈️';
      if (n.includes('shop') || n.includes('souvenir') || n.includes('gift')) return '🛍️';
      return '💰';
    };

    expenses.forEach(exp => {
      const dateStr = exp.created_at ? new Date(exp.created_at).toLocaleDateString('en-IN', { day:'numeric', month:'short' }) : 'Today';
      const row = document.createElement('div');
      row.className = 'budget-expense-row';
      row.innerHTML = `
        <div class="budget-expense-icon">${emojiFor(exp.note || '')}</div>
        <div class="budget-expense-info">
          <span class="budget-expense-note">${exp.note || 'Expense'}</span>
          <span class="budget-expense-date">${dateStr}</span>
        </div>
        <span class="budget-expense-amount">-₹${exp.amount.toFixed(0)}</span>
        <button class="budget-expense-del" aria-label="Delete">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="3 6 5 6 21 6"></polyline>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
          </svg>
        </button>
      `;

      row.querySelector('.budget-expense-del').addEventListener('click', () => {
        fetch(`${BASE_URL}/budget.php?action=delete_expense&trip=${encodeURIComponent(trip)}&id=${exp.id}`)
          .then(r => r.json())
          .then(d => { if (d.status === 'success') loadBudgetTracker(trip); });
      });

      expList.appendChild(row);
    });
  }

  // Weather Overlay Core Integration
  function loadWeatherOverlay(trip) {
    const hourlyContainer = document.getElementById('weather-overlay-hourly');
    const dailyContainer = document.getElementById('weather-overlay-daily');

    hourlyContainer.innerHTML = `<div class="empty-feed-state" style="padding:10px;"><div class="dot-pulse"></div></div>`;
    dailyContainer.innerHTML = `<div class="empty-feed-state" style="padding:10px;"><div class="dot-pulse"></div></div>`;

    fetch(`${BASE_URL}/weather.php?place=${encodeURIComponent(trip)}`)
      .then(res => res.json())
      .then(res => {
        if (res.current_temp) {
          document.getElementById('weather-overlay-condition').textContent = res.condition || 'Sunny';
          document.getElementById('weather-overlay-temp').textContent = `${res.current_temp}`;

          const getEmoji = (cond) => {
            const c = cond.toLowerCase();
            if (c.includes('sun') || c.includes('clear')) return '☀️';
            if (c.includes('rain') || c.includes('drizzle')) return '🌧️';
            if (c.includes('cloud')) return '☁️';
            if (c.includes('storm') || c.includes('thunder')) return '⛈️';
            return '⛅';
          };

          // Render Hourly
          hourlyContainer.innerHTML = '';
          (res.hourly || []).forEach(h => {
            const card = document.createElement('div');
            card.className = 'hourly-forecast-card';
            card.innerHTML = `
              <div class="hourly-time">${h.time}</div>
              <div class="hourly-emoji">${getEmoji(h.condition)}</div>
              <div class="hourly-temp">${h.temp}</div>
            `;
            hourlyContainer.appendChild(card);
          });

          // Render Daily
          dailyContainer.innerHTML = '';
          (res.daily || []).forEach(d => {
            const row = document.createElement('div');
            row.className = 'daily-forecast-row';
            row.innerHTML = `
              <div class="daily-day">${d.day}</div>
              <div class="daily-row-middle">
                <span class="daily-emoji">${getEmoji(d.condition)}</span>
                <span class="daily-condition-lbl">${d.condition}</span>
              </div>
              <div class="daily-temp-range">${d.low} / ${d.high}</div>
            `;
            dailyContainer.appendChild(row);
          });
        }
      });
  }

  // Set default active traveler values in localStorage on onboarding
  if (!localStorage.getItem('ACTIVE_USER_NAME')) {
    localStorage.setItem('ACTIVE_USER_NAME', 'Aarav Mehta');
    localStorage.setItem('ACTIVE_EMAIL', 'aarav.mehta@gmail.com');
  }

}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initTravelBuddyApp);
} else {
  initTravelBuddyApp();
}


// Dynamic styles for the premium inline spinner used in button loading state
const style = document.createElement('style');
style.textContent = `
  @keyframes rotate {
    100% { transform: rotate(360deg); }
  }
  @keyframes dash {
    0% { stroke-dasharray: 1, 150; stroke-dashoffset: 0; }
    50% { stroke-dasharray: 90, 150; stroke-dashoffset: -35; }
    100% { stroke-dasharray: 90, 150; stroke-dashoffset: -124; }
  }
`;
document.head.appendChild(style);
