import { useProfile } from "../features/profile/useProfile";
import type { FilterKey } from "../types/domain";
import ProfileIdentity from "../components/profile/ProfileIdentity";
import DocumentSummary from "../components/profile/DocumentSummary";
import ProfilePreferences from "../components/profile/ProfilePreferences";
import PrivacyHelp from "../components/profile/PrivacyHelp";
import AccountActions from "../components/profile/AccountActions";
import LoggedOut from "../components/profile/LoggedOut";
export default function ProfilePage(props: {
  onBack: () => void;
  onOpenAllDocs: (filter?: FilterKey) => void;
}) {
  const model = useProfile(props);
  if (model.loggedOut) return <LoggedOut model={model} />;
  return (
    <div style={model.s.page}>
      {model.profileError && (
        <p role="alert" className="feedback">
          {model.profileError}
        </p>
      )}
      <ProfileIdentity model={model} />
      <DocumentSummary model={model} />
      <ProfilePreferences model={model} />
      <PrivacyHelp model={model} />
      <AccountActions model={model} />
    </div>
  );
}
