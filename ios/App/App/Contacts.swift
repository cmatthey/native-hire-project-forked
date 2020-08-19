import Foundation
import Capacitor
import Contacts

@objc(Contacts)
public class Contacts : CAPPlugin {
    // TODO: return error
    @objc func getAll(_ call: CAPPluginCall) {
        let contacts = getContactsHelper(keyword: "")
        call.success([
            "contacts": contacts
        ])
    }
    
    @objc func queryContacts(_ call: CAPPluginCall) {
        let keyword: String = call.getString("keyword") ?? ""
        let contacts = getContactsHelper(keyword: keyword)
        call.success([
            "contacts": contacts
        ])
    }
    
    // Part 1: Add the functionality to retrieve all the contacts from the device on iOS.
    // Part 2: Add a method to the contacts plugin that allows for querying contacts on the device.
    private func getContactsHelper(keyword: String) -> [[String: Any]] {
        var contacts: [[String: Any]] = []
        let store = CNContactStore()
        store.requestAccess(for: .contacts) { (granted, error) in
            if let error = error {
                print("Failed to request access: \(error)")
                return
            }
            if granted {
                let keys = [CNContactGivenNameKey, CNContactFamilyNameKey, CNContactPhoneNumbersKey, CNContactEmailAddressesKey]
                if keyword.isEmpty {
                    // Part 1: Add the functionality to retrieve all the contacts from the device on iOS.
                    do {
                        let request = CNContactFetchRequest(keysToFetch: keys as [CNKeyDescriptor])
                        try store.enumerateContacts(with: request, usingBlock: { (contact, stopPointer) in
                            let phoneNumbers: [String] = contact.phoneNumbers.map{ $0.value.stringValue }
                            let emailAddress = contact.emailAddresses.map{ $0.value }
                            contacts.append(["firstName": contact.givenName, "lastName": contact.familyName, "phoneNumbers": phoneNumbers, "emailAddresses": emailAddress])
                        })
                    } catch let error {
                        print("Failed to enumerate contact: \(error)")
                    }
                } else {
                    // Part 2: Add a method to the contacts plugin that allows for querying contacts on the device.
                    do {
                        let predicate = CNContact.predicateForContacts(matchingName: keyword)
                        let unifiedContacts = try store.unifiedContacts(matching: predicate, keysToFetch: keys as [CNKeyDescriptor])
                        for contact in unifiedContacts {
                                let phoneNumbers: [String] = contact.phoneNumbers.map{ $0.value.stringValue }
                                let emailAddress = contact.emailAddresses.map{ $0.value }
                                contacts.append(["firstName": contact.givenName, "lastName": contact.familyName, "phoneNumbers": phoneNumbers, "emailAddresses": emailAddress])
                        }
                    } catch {
                        print("Failed to fetch contact, error: \(error)")
                    }
                }
            } else {
                print("Access denied")
            }
        }
        return contacts
    }
    
    private func getAllMocked() -> [Any] {
        return [
            [
                "firstName": "Elton",
                "lastName": "Json",
                "phoneNumbers": ["2135551111"],
                "emailAddresses": ["elton@eltonjohn.com"],
            ],
            [
                "firstName": "Freddie",
                "lastName": "Mercury",
                "phoneNumbers": [],
                "emailAddresses": [],
            ],
        ]
    }
}
