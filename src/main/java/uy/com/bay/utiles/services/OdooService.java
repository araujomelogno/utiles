package uy.com.bay.utiles.services;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import uy.com.bay.utiles.config.OdooConfig;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// java.util.Vector is not directly used in the provided code, so I'm omitting it for now.
// If it's needed by a dependency or a future version, it can be added.

@Service
public class OdooService {

	private static final Logger logger = LoggerFactory.getLogger(OdooService.class);
	private final OdooConfig odooConfig;
	private XmlRpcClient objectClient; // For model operations
	private XmlRpcClient commonClient; // For authentication

	public OdooService(OdooConfig odooConfig) {
		this.odooConfig = odooConfig;
		try {
			XmlRpcClientConfigImpl commonConfig = new XmlRpcClientConfigImpl();
			commonConfig.setServerURL(new URL(odooConfig.getUrl() + "/xmlrpc/2/common"));
			logger.info("Odoo Common API URL configured: {}", commonConfig.getServerURL());
			System.out.println("Odoo Common API URL configured: {}" + commonConfig.getServerURL());
			commonClient = new XmlRpcClient();
			commonClient.setConfig(commonConfig);
			// It's good practice to set connection and read timeouts
			// commonConfig.setConnectionTimeout(60 * 1000); // 60 seconds
			// commonConfig.setReplyTimeout(60 * 1000); // 60 seconds

			XmlRpcClientConfigImpl objectConfig = new XmlRpcClientConfigImpl();
			objectConfig.setServerURL(new URL(odooConfig.getUrl() + "/xmlrpc/2/object"));
			logger.info("Odoo Object API URL configured: {}", objectConfig.getServerURL());
			System.out.println("Odoo Object API URL configured: {}" + objectConfig.getServerURL());
			objectClient = new XmlRpcClient();
			objectClient.setConfig(objectConfig);
			// It's good practice to set connection and read timeouts
			// objectConfig.setConnectionTimeout(60 * 1000); // 60 seconds
			// objectConfig.setReplyTimeout(60 * 1000); // 60 seconds

		} catch (MalformedURLException e) {
			logger.error("Malformed Odoo URL: {}", odooConfig.getUrl(), e);
			// Consider how to handle this state - perhaps the service should not be usable.
			// For now, subsequent calls will likely fail if clients are null.
			throw new RuntimeException("Error initializing Odoo XML-RPC client: Invalid URL", e);
		}
	}

	private Integer authenticate() throws XmlRpcException {
		if (commonClient == null) {
			logger.error("Odoo common client not initialized. Cannot authenticate.");
			throw new IllegalStateException("Odoo common client not initialized.");
		}
		Object result = commonClient.execute("authenticate", Arrays.asList(odooConfig.getDb(), odooConfig.getUsername(),
				odooConfig.getPassword(), Collections.emptyMap()));
		if (result instanceof Integer) {
			Integer uid = (Integer) result;
			if (uid != 0) { // Odoo returns 0 or false for failed login, uid > 0 for success
				logger.info("Successfully authenticated with Odoo. UID: {}", uid);
				return uid;
			}
		}
		logger.error("Odoo authentication failed. Result: {}. Check credentials, DB name, and Odoo URL.", result);
		return null; // Or throw a specific authentication exception
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getOdooProjects() {
		if (objectClient == null) {
			logger.error("Odoo object client not initialized. Cannot fetch projects.");
			return Collections.emptyList();
		}
		try {
			Integer uid = authenticate();
			if (uid == null) {
				logger.error("Cannot fetch projects: Authentication failed.");
				return Collections.emptyList();
			}

			List<String> fieldsToFetch = Arrays.asList("id", "name");
			List<Object> domain = Collections.emptyList(); // Fetch all projects

			HashMap<String, Object> keywordArgs = new HashMap<>();
			keywordArgs.put("fields", fieldsToFetch);
			// keywordArgs.put("limit", 10); // Optional: for pagination
			// keywordArgs.put("offset", 0); // Optional: for pagination

			Object[] params = new Object[] { odooConfig.getDb(), uid, odooConfig.getPassword(), "project.project", // Odoo
																													// model
																													// name
																													// for
																													// projects
					"search_read", // Method to call
					Collections.singletonList(domain), keywordArgs };

			logger.info("Executing Odoo search_read on 'project.project' with fields: {}", fieldsToFetch);
			Object[] projectsRaw = (Object[]) objectClient.execute("execute_kw", params);

			List<Map<String, Object>> projectsList = new ArrayList<>();
			for (Object projectObj : projectsRaw) {
				if (projectObj instanceof Map) {
					projectsList.add((Map<String, Object>) projectObj);
				} else {
					logger.warn("Received an object that is not a Map from Odoo: {}", projectObj);
				}
			}
			logger.info("Successfully fetched {} projects from Odoo.", projectsList.size());

			return projectsList;

		} catch (XmlRpcException e) {
			logger.error(
					"XmlRpcException while fetching projects from Odoo: {}. Check Odoo XML-RPC endpoint and network.",
					e.getMessage(), e);
		} catch (ClassCastException e) {
			logger.error("ClassCastException while processing Odoo response. Unexpected data structure: {}",
					e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Unexpected exception while fetching projects from Odoo: {}", e.getMessage(), e);
		}
		return Collections.emptyList(); // Return empty list in case of any error
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getOdooLeads() {
		if (objectClient == null) {
			logger.error("Odoo object client not initialized. Cannot fetch leads.");
			return Collections.emptyList();
		}
		try {
			Integer uid = authenticate();
			if (uid == null) {
				logger.error("Cannot fetch leads: Authentication failed.");
				return Collections.emptyList();
			}

			List<String> fieldsToFetch = Arrays.asList("id", "name");
			List<Object> domain = Arrays.asList(Arrays.asList("name", "=like", "S%"));

			HashMap<String, Object> keywordArgs = new HashMap<>();
			keywordArgs.put("fields", fieldsToFetch);
			// keywordArgs.put("limit", 10); // Optional: for pagination
			// keywordArgs.put("offset", 0); // Optional: for pagination

			Object[] params = new Object[] { odooConfig.getDb(), uid, odooConfig.getPassword(), "crm.lead", // Odoo
																											// model
																											// name for
																											// leads
					"search_read", // Method to call
					Collections.singletonList(domain), keywordArgs };

			logger.info("Executing Odoo search_read on 'crm.lead' with fields: {}", fieldsToFetch);
			Object[] leadsRaw = (Object[]) objectClient.execute("execute_kw", params);

			List<Map<String, Object>> leadsList = new ArrayList<>();
			for (Object leadObj : leadsRaw) {
				if (leadObj instanceof Map) {
					leadsList.add((Map<String, Object>) leadObj);
				} else {
					logger.warn("Received an object that is not a Map from Odoo: {}", leadObj);
				}
			}
			logger.info("Successfully fetched {} leads from Odoo.", leadsList.size());

			return leadsList;

		} catch (XmlRpcException e) {
			logger.error("XmlRpcException while fetching leads from Odoo: {}. Check Odoo XML-RPC endpoint and network.",
					e.getMessage(), e);
		} catch (ClassCastException e) {
			logger.error("ClassCastException while processing Odoo response. Unexpected data structure: {}",
					e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Unexpected exception while fetching leads from Odoo: {}", e.getMessage(), e);
		}
		return Collections.emptyList(); // Return empty list in case of any error
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getOdooAnalyticAccounts() {
		if (objectClient == null) {
			logger.error("Odoo object client not initialized. Cannot fetch analytic accounts.");
			return Collections.emptyList();
		}
		try {
			Integer uid = authenticate();
			if (uid == null) {
				logger.error("Cannot fetch analytic accounts: Authentication failed.");
				return Collections.emptyList();
			}

			List<String> fieldsToFetch = Arrays.asList("id", "name", "partner_id");
			List<Object> domain = Collections.emptyList(); // Fetch all analytic accounts

			HashMap<String, Object> keywordArgs = new HashMap<>();
			keywordArgs.put("fields", fieldsToFetch);

			Object[] params = new Object[] { odooConfig.getDb(), uid, odooConfig.getPassword(),
					"account.analytic.account", // Odoo model name for analytic accounts
					"search_read", // Method to call
					Collections.singletonList(domain), keywordArgs };

			logger.info("Executing Odoo search_read on 'account.analytic.account' with fields: {}", fieldsToFetch);
			Object[] accountsRaw = (Object[]) objectClient.execute("execute_kw", params);

			List<Map<String, Object>> accountsList = new ArrayList<>();
			for (Object accountObj : accountsRaw) {
				if (accountObj instanceof Map) {
					Map<String, Object> accountMap = (Map<String, Object>) accountObj;

					String clientName = null;
					Object partnerIdObj = accountMap.get("partner_id");
					if (partnerIdObj instanceof Object[]) {
						Object[] partnerArr = (Object[]) partnerIdObj;
						if (partnerArr.length >= 2 && partnerArr[1] != null) {
							clientName = String.valueOf(partnerArr[1]);
						}
					} else if (partnerIdObj instanceof List) {
						List<Object> partnerList = (List<Object>) partnerIdObj;
						if (partnerList.size() >= 2 && partnerList.get(1) != null) {
							clientName = String.valueOf(partnerList.get(1));
						}
					}
					accountMap.put("client_name", clientName);

					Object nameObj = accountMap.get("name");
					if (nameObj != null) {
						Double expectedRevenue = findLeadExpectedRevenueByName(uid, String.valueOf(nameObj));
						if (expectedRevenue != null) {
							accountMap.put("expected_revenue", expectedRevenue);
						}
					}

					accountsList.add(accountMap);
				} else {
					logger.warn("Received an object that is not a Map from Odoo: {}", accountObj);
				}
			}
			logger.info("Successfully fetched {} analytic accounts from Odoo.", accountsList.size());

			return accountsList;

		} catch (XmlRpcException e) {
			logger.error(
					"XmlRpcException while fetching analytic accounts from Odoo: {}. Check Odoo XML-RPC endpoint and network.",
					e.getMessage(), e);
		} catch (ClassCastException e) {
			logger.error("ClassCastException while processing Odoo response. Unexpected data structure: {}",
					e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Unexpected exception while fetching analytic accounts from Odoo: {}", e.getMessage(), e);
		}
		return Collections.emptyList(); // Return empty list in case of any error
	}

	@SuppressWarnings("unchecked")
	private Double findLeadExpectedRevenueByName(Integer uid, String leadName) {
		if (leadName == null || leadName.trim().isEmpty()) {
			return null;
		}
		try {
			List<Object> domain = Collections.singletonList(Arrays.asList("name", "=", leadName));

			HashMap<String, Object> keywordArgs = new HashMap<>();
			keywordArgs.put("fields", Arrays.asList("id", "name", "expected_revenue"));
			keywordArgs.put("limit", 1);

			Object[] params = new Object[] { odooConfig.getDb(), uid, odooConfig.getPassword(), "crm.lead",
					"search_read", Collections.singletonList(domain), keywordArgs };

			Object[] leadsRaw = (Object[]) objectClient.execute("execute_kw", params);
			if (leadsRaw == null || leadsRaw.length == 0) {
				return null;
			}
			Object first = leadsRaw[0];
			if (first instanceof Map) {
				Object revenue = ((Map<String, Object>) first).get("expected_revenue");
				if (revenue instanceof Number) {
					return ((Number) revenue).doubleValue();
				}
			}
		} catch (XmlRpcException e) {
			logger.error("XmlRpcException while searching crm.lead by name '{}': {}", leadName, e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Unexpected exception while searching crm.lead by name '{}': {}", leadName, e.getMessage(), e);
		}
		return null;
	}
	
	
	
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getOdooProducts() {
		if (objectClient == null) {
			logger.error("Odoo object client not initialized. Cannot fetch analytic accounts.");
			return Collections.emptyList();
		}
		try {
			Integer uid = authenticate();
			if (uid == null) {
				logger.error("Cannot fetch analytic accounts: Authentication failed.");
				return Collections.emptyList();
			}

			List<String> fieldsToFetch = Arrays.asList("id", "name");
			List<Object> domain = Collections.emptyList(); // Fetch all analytic accounts

			HashMap<String, Object> keywordArgs = new HashMap<>();
			keywordArgs.put("fields", fieldsToFetch);

			Object[] params = new Object[] { odooConfig.getDb(), uid, odooConfig.getPassword(),
					"product.product", // Odoo model name for analytic accounts
					"search_read", // Method to call
					Collections.singletonList(domain), keywordArgs };

			logger.info("Executing Odoo search_read on 'account.analytic.account' with fields: {}", fieldsToFetch);
			Object[] accountsRaw = (Object[]) objectClient.execute("execute_kw", params);

			List<Map<String, Object>> accountsList = new ArrayList<>();
			for (Object accountObj : accountsRaw) {
				if (accountObj instanceof Map) {
					accountsList.add((Map<String, Object>) accountObj);
				} else {
					logger.warn("Received an object that is not a Map from Odoo: {}", accountObj);
				}
			}
			logger.info("Successfully fetched {} analytic accounts from Odoo.", accountsList.size());

			return accountsList;

		} catch (XmlRpcException e) {
			logger.error(
					"XmlRpcException while fetching analytic accounts from Odoo: {}. Check Odoo XML-RPC endpoint and network.",
					e.getMessage(), e);
		} catch (ClassCastException e) {
			logger.error("ClassCastException while processing Odoo response. Unexpected data structure: {}",
					e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Unexpected exception while fetching analytic accounts from Odoo: {}", e.getMessage(), e);
		}
		return Collections.emptyList(); // Return empty list in case of any error
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getOdooAccountMoveLines(String analitycAccountId, String productId, LocalDate init,
			LocalDate end) {
		if (objectClient == null) {
			logger.error("Odoo object client not initialized. Cannot fetch account move lines.");
			return Collections.emptyList();
		}
		try {
			Integer uid = authenticate();
			if (uid == null) {
				logger.error("Cannot fetch account move lines: Authentication failed.");
				return Collections.emptyList();
			}

			List<String> fieldsToFetch = Arrays.asList("id", "date", "move_id", "name", "product_id", "account_id",
					"debit", "credit", "balance");

			List<Object> domain = new ArrayList<>();
			domain.add(Arrays.asList("product_id", "=", Integer.parseInt(productId)));
			domain.add(Arrays.asList("analytic_account_id", "=", Integer.parseInt(analitycAccountId)));
			if (init != null && end != null) {
				domain.add(Arrays.asList("date", ">=", init.toString()));
				domain.add(Arrays.asList("date", "<=", end.toString()));
			}

			HashMap<String, Object> keywordArgs = new HashMap<>();
			keywordArgs.put("fields", fieldsToFetch);

			Object[] params = new Object[] { odooConfig.getDb(), uid, odooConfig.getPassword(), "account.move.line",
					"search_read", Collections.singletonList(domain), keywordArgs };

			logger.info("Executing Odoo search_read on 'account.move.line' with fields: {}", fieldsToFetch);
			Object[] linesRaw = (Object[]) objectClient.execute("execute_kw", params);

			List<Map<String, Object>> linesList = new ArrayList<>();
			for (Object lineObj : linesRaw) {
				if (lineObj instanceof Map) {
					linesList.add((Map<String, Object>) lineObj);
				} else {
					logger.warn("Received an object that is not a Map from Odoo: {}", lineObj);
				}
			}
			logger.info("Successfully fetched {} account move lines from Odoo.", linesList.size());

			return linesList;

		} catch (XmlRpcException e) {
			logger.error(
					"XmlRpcException while fetching account move lines from Odoo: {}. Check Odoo XML-RPC endpoint and network.",
					e.getMessage(), e);
		} catch (ClassCastException e) {
			logger.error("ClassCastException while processing Odoo response. Unexpected data structure: {}",
					e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Unexpected exception while fetching account move lines from Odoo: {}", e.getMessage(), e);
		}
		return Collections.emptyList();
	}

}
